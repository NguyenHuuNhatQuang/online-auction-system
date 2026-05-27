package com.auction.server.services;

import com.auction.common.models.Auction;
import com.auction.server.database.AuctionDAO;
import com.auction.server.database.DatabaseWriteQueue;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

  private static volatile AuctionManager instance;
  private final ConcurrentHashMap<String, Auction> auctions;
  private final AuctionDAO auctionDAO;

  private AuctionManager() {
    this.auctions = new ConcurrentHashMap<>();
    this.auctionDAO = new AuctionDAO();
    // TỰ ĐỘNG KHÔI PHỤC TOÀN BỘ PHIÊN TỪ DATABASE LÊN RAM KHI BẬT SERVER
    loadAuctionsFromDatabase();
  }

  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) {
          instance = new AuctionManager();
        }
      }
    }
    return instance;
  }

  private void loadAuctionsFromDatabase() {
    try {
      Collection<Auction> dbAuctions = auctionDAO.getAllAuctions();
      com.auction.server.database.BidTransactionDAO bidDAO = new com.auction.server.database.BidTransactionDAO(); // KHỞI TẠO DAO

      for (Auction a : dbAuctions) {
        // 1. Lấy toàn bộ lịch sử của phiên này từ DB
        java.util.List<com.auction.common.models.BidTransaction> history = bidDAO.getBidsByAuction(a.getId());

        // 2. Bơm vào đối tượng Auction (Giả sử lớp Auction của bạn có method setBidHistory hoặc addBid)
        // Nếu lớp Auction chưa có, bạn hãy thêm List<BidTransaction> bidHistory vào model Auction nhé.
        if (history != null && !history.isEmpty()) {
          for(com.auction.common.models.BidTransaction tx : history) {
            a.addBidTransaction(tx); // Thêm hàm này vào class Auction nếu chưa có
          }
        }

        auctions.put(a.getId(), a);
      }
      System.out.println("[AuctionManager] Đồng bộ thành công " + dbAuctions.size() + " phiên đấu giá từ SQLite lên bộ nhớ RAM.");
    } catch (Exception e) {
      System.err.println("[AuctionManager] Lỗi khởi chạy khôi phục dữ liệu: " + e.getMessage());
    }
  }

  public void addAuction(Auction auction) {
    // 1. Ghi vào bộ nhớ đệm RAM phục vụ kết nối mạng realtime lập tức
    auctions.put(auction.getId(), auction);

    // 2. Xếp hàng tác vụ I/O xuống đĩa cứng
    DatabaseWriteQueue.getInstance().execute(() -> {
      try {
        auctionDAO.insertAuction(auction);
      } catch (Exception e) {
        System.err.println("[AuctionManager] Lỗi ghi phiên đấu giá mới vào DB: " + e.getMessage());
      }
    });
  }

  public Auction getAuction(String auctionId) {
    return auctions.get(auctionId);
  }

  public Collection<Auction> getAllAuctions() {
    return auctions.values();
  }

  public void updateAuctionStatus(String auctionId, String status) {
    Auction auction = auctions.get(auctionId);
    if (auction != null) {
      // Đổi trạng thái trên RAM
      auction.setStatus(status);

      // Đồng bộ bất đồng bộ xuống DB
      DatabaseWriteQueue.getInstance().execute(() -> {
        try {
          auctionDAO.updateAuctionStatus(auctionId, status);
        } catch (Exception e) {
          System.err.println("[AuctionManager] Lỗi lưu trạng thái phiên " + status + " vào DB: " + e.getMessage());
        }
      });
    }
  }

  /**
   * HÀM DÀNH RIÊNG CHO UNIT TEST.
   * Xóa sạch bộ nhớ đệm trên RAM để bài test mới không bị dính dữ liệu cũ.
   */
  public void clearCacheForTesting() {
    auctions.clear();
  }
}