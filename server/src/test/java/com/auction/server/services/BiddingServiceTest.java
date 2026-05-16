package com.auction.server.services;

import com.auction.common.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho BiddingService.
 * Đảm bảo logic nghiệp vụ và khả năng xử lý đồng thời (Thread-safe) hoạt động đúng.
 */
class BiddingServiceTest {

  private BiddingService biddingService;
  private AuctionManager auctionManager;
  private Auction testAuction;
  private Bidder testBidder;

  @BeforeEach
  void setUp() {
    // Reset Singleton và Service trước mỗi test case
    auctionManager = AuctionManager.getInstance();
    biddingService = new BiddingService();

    // Tạo dữ liệu giả (Mock data)
    Item item = new Electronics("item1", "iPhone 15", "Brand new", 12);
    Seller seller = new Seller("seller1", "john_doe", "password123");
    testBidder = new Bidder("bidder1", "alice", "pass456");

    // Tạo phiên đấu giá bắt đầu từ hiện tại và kết thúc sau 1 giờ
    testAuction = new Auction(
        "auction1", item, seller, 1000.0,
        LocalDateTime.now(), LocalDateTime.now().plusHours(1)
    );
    testAuction.setStatus("RUNNING");

    // Đưa phiên đấu giá vào bộ nhớ trung tâm
    auctionManager.addAuction(testAuction);
  }

  @Test
  @DisplayName("Đặt giá thành công với số tiền hợp lệ")
  void testPlaceBid_Success() throws Exception {
    boolean result = biddingService.placeBid("auction1", testBidder, 1200.0);

    assertTrue(result, "Hàm placeBid phải trả về true khi thành công");
    assertEquals(1200.0, testAuction.getCurrentPrice(), "Giá hiện tại phải được cập nhật thành 1200.0");
    assertEquals(testBidder, testAuction.getHighestBidder(), "Người dẫn đầu phải là alice");
  }

  @Test
  @DisplayName("Ném ngoại lệ khi đặt giá thấp hơn hoặc bằng giá hiện tại")
  void testPlaceBid_Fail_PriceTooLow() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      biddingService.placeBid("auction1", testBidder, 900.0);
    });

    // Đã sửa lại chuỗi kiểm tra cho khớp với Exception thực tế
    assertTrue(exception.getMessage().contains("phải cao hơn giá hiện tại"));
  }

  @Test
  @DisplayName("Ném ngoại lệ khi đặt giá vào phiên đã kết thúc")
  void testPlaceBid_Fail_AuctionClosed() {
    testAuction.setStatus("FINISHED");

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      biddingService.placeBid("auction1", testBidder, 1500.0);
    });

    assertTrue(exception.getMessage().contains("không trong trạng thái chấp nhận"));
  }

  @Test
  @DisplayName("Kiểm thử Đấu giá Đồng thời (Concurrency/Race Condition)")
  void testConcurrentBidding() throws InterruptedException {
    int numberOfThreads = 50;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

    // Sử dụng CountDownLatch để "giữ chân" các thread, sau đó thả ra cùng 1 lúc (mô phỏng đồng thời)
    CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

    AtomicInteger successfulBids = new AtomicInteger(0);
    AtomicInteger failedBids = new AtomicInteger(0);

    // Tạo 50 Bidders cùng cố gắng đặt giá 1500.0 vào cùng 1 mili-giây
    for (int i = 0; i < numberOfThreads; i++) {
      Bidder concurrentBidder = new Bidder("bidder" + i, "user" + i, "pass");
      executorService.execute(() -> {
        try {
          readyLatch.countDown(); // Báo cáo thread đã sẵn sàng
          startLatch.await();     // Chờ hiệu lệnh bắt đầu

          biddingService.placeBid("auction1", concurrentBidder, 1500.0);
          successfulBids.incrementAndGet(); // Chỉ 1 thread thành công
        } catch (Exception e) {
          failedBids.incrementAndGet(); // 49 thread còn lại phải bị bật ra (do giá bị khóa/đã thay đổi)
        } finally {
          doneLatch.countDown(); // Báo cáo thread đã chạy xong
        }
      });
    }

    readyLatch.await(); // Đợi cả 50 thread sẵn sàng
    startLatch.countDown(); // PHÁT LỆNH CHẠY ĐỒNG THỜI!
    doneLatch.await(); // Đợi cả 50 thread chạy xong để kiểm tra kết quả

    // Kiểm tra tính toàn vẹn của dữ liệu
    assertEquals(1, successfulBids.get(), "Chỉ duy nhất 1 luồng được phép đặt giá 1500.0 thành công");
    assertEquals(49, failedBids.get(), "49 luồng còn lại phải bị từ chối do xung đột giá");
    assertEquals(1500.0, testAuction.getCurrentPrice(), "Giá cuối cùng phải chính xác là 1500.0");

    executorService.shutdown();
  }
}