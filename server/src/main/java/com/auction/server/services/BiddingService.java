package com.auction.server.services;

import com.auction.common.models.Auction;
import com.auction.common.models.BidTransaction;
import com.auction.common.models.Bidder;
import com.auction.server.database.DatabaseWriteQueue;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dịch vụ xử lý nghiệp vụ đặt giá (Bidding).
 * Đảm bảo tính toàn vẹn dữ liệu và xử lý đồng thời (Concurrency) an toàn khi nhiều người cùng đặt giá.
 */
public class BiddingService {

  private final AuctionManager auctionManager;

  // Lưu trữ khóa (Lock) riêng biệt cho từng phiên đấu giá
  private final ConcurrentHashMap<String, ReentrantLock> auctionLocks;
  private final com.auction.server.database.BidTransactionDAO bidTransactionDAO = new com.auction.server.database.BidTransactionDAO();
  private final com.auction.server.database.AuctionDAO auctionDAO = new com.auction.server.database.AuctionDAO();

  /**
   * Khởi tạo BiddingService.
   */
  public BiddingService() {
    this.auctionManager = AuctionManager.getInstance();
    this.auctionLocks = new ConcurrentHashMap<>();
  }

  /**
   * Lấy đối tượng khóa (Lock) cho một phiên đấu giá cụ thể.
   *
   * @param auctionId ID của phiên đấu giá.
   * @return ReentrantLock tương ứng với phiên.
   */
  private ReentrantLock getLockForAuction(String auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
  }

  /**
   * Thực hiện hành động đặt giá của một Bidder vào một phiên đấu giá.
   *
   * @param auctionId ID của phiên đấu giá.
   * @param bidder    Người tham gia đặt giá.
   * @param bidAmount Số tiền đặt giá.
   * @return true nếu đặt giá thành công.
   * @throws IllegalArgumentException Nếu phiên không tồn tại hoặc giá không hợp lệ.
   * @throws IllegalStateException    Nếu trạng thái/thời gian phiên không cho phép đặt giá.
   */
  public boolean placeBid(String auctionId, Bidder bidder, double bidAmount) {
    Auction auction = auctionManager.getAuction(auctionId);
    if (auction == null) {
      throw new IllegalArgumentException("Phiên đấu giá không tồn tại.");
    }

    ReentrantLock lock = getLockForAuction(auctionId);

    // Cấp khóa (Lock). Chỉ duy nhất 1 luồng được phép đi qua dòng này tại 1 thời điểm cho phiên đấu giá này.
    lock.lock();
    try {
      // 1. Kiểm tra thời gian
      if (LocalDateTime.now().isAfter(auction.getEndTime())) {
        auction.setStatus("FINISHED");
        throw new IllegalStateException("Phiên đấu giá đã kết thúc do hết thời gian quy định.");
      }

      // 2. Kiểm tra trạng thái
      if (!"RUNNING".equals(auction.getStatus())) {
        throw new IllegalStateException("Phiên đấu giá hiện không trong trạng thái chấp nhận lượt đặt giá.");
      }

      // 3. Kiểm tra tính hợp lệ của giá đặt (Phải lớn hơn giá hiện tại)
      if (bidAmount <= auction.getCurrentPrice()) {
        throw new IllegalArgumentException(
            "Giá đặt (" + bidAmount + ") phải cao hơn giá hiện tại (" + auction.getCurrentPrice() + ")."
        );
      }

      // 4. Cập nhật dữ liệu một cách an toàn (Race Condition đã được ngăn chặn)
      auction.setCurrentPrice(bidAmount);
      auction.setHighestBidder(bidder);

      // 5. Ghi nhận giao dịch lịch sử
      BidTransaction transaction = new BidTransaction(
          UUID.randomUUID().toString(),
          auctionId,
          bidder,
          bidAmount,
          LocalDateTime.now()
      );

      String transactionId = "TX_" + java.util.UUID.randomUUID().toString().substring(0, 8);
      com.auction.common.models.BidTransaction bidTx = new com.auction.common.models.BidTransaction(transactionId, auctionId, bidder, bidAmount, java.time.LocalDateTime.now());

      auction.addBidTransaction(bidTx);

      DatabaseWriteQueue.getInstance().execute(() -> {
        try {
          new com.auction.server.database.BidTransactionDAO().insertBid(bidTx);
          new com.auction.server.database.AuctionDAO().updateAuctionPriceAndBidder(auctionId, bidAmount, bidder.getId());
          System.out.println("[BiddingService] Đã lưu giao dịch nâng giá của " + bidder.getUsername() + " vào SQLite.");
        } catch (Exception e) {
          System.err.println("[BiddingService] Lỗi ghi nhật ký đặt giá vào DB: " + e.getMessage());
        }
      });
      System.out.println("[BiddingService] Người dùng " + bidder.getUsername() +
          " đã đặt giá thành công " + bidAmount + " cho phiên " + auctionId);

      return true;

    } finally {
      // LUÔN LUÔN mở khóa trong block finally để tránh deadlock nếu có lỗi xảy ra
      lock.unlock();
    }
  }

  /**
   * Kiểm tra và tự động đóng phiên đấu giá nếu đã hết thời gian.
   * Sử dụng cơ chế tryLock() để không làm treo luồng quét nếu có người đang đặt giá.
   *
   * @param auctionId ID của phiên đấu giá cần kiểm tra.
   * @return Trả về đối tượng Auction nếu nó VỪA MỚI được đóng thành công, ngược lại trả về null.
   */
  public Auction checkAndCloseIfExpired(String auctionId) {
    Auction auction = auctionManager.getAuction(auctionId);

    // Bỏ qua nhanh nếu không tồn tại hoặc đã đóng
    if (auction == null || !"RUNNING".equals(auction.getStatus())) {
      return null;
    }

    ReentrantLock lock = getLockForAuction(auctionId);

    // Sử dụng tryLock(): Nếu có Bidder nào đó đang giữ khóa để đặt giá,
    // Scheduler sẽ không đứng chờ mà lập tức bỏ qua (sẽ kiểm tra lại ở giây tiếp theo).
    if (lock.tryLock()) {
      try {
        // Kiểm tra lại trạng thái và thời gian sau khi đã vào trong vùng an toàn (Critical Section)
        if ("RUNNING".equals(auction.getStatus()) && LocalDateTime.now().isAfter(auction.getEndTime())) {
          auction.setStatus("FINISHED");
          return auction; // Trả về để Scheduler biết mà thông báo cho Client
        }
      } finally {
        lock.unlock(); // Luôn giải phóng khóa
      }
    }

    return null;
  }
}