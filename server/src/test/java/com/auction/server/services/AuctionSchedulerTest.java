package com.auction.server.services;

import com.auction.common.models.Auction;
import com.auction.common.models.Bidder;
import com.auction.common.models.Electronics;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;
import com.auction.server.network.AuctionServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho AuctionScheduler.
 * Sử dụng Mockito để giả lập các phụ thuộc và Reflection để kiểm thử logic private.
 */
@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

  @Mock
  private BiddingService mockBiddingService;

  @Mock
  private AuctionServer mockServer;

  private AuctionScheduler scheduler;
  private AuctionManager auctionManager;
  private Auction testAuction;

  @BeforeEach
  void setUp() throws Exception {
    // 1. Dọn dẹp sạch sẽ Hàng đợi và Database để tránh lỗi khóa file hoặc trùng ID
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();
    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("auction_system.db"));
    com.auction.server.database.DatabaseConnection.initDatabase();

    // 2. Lấy Singleton instance và dọn dẹp trạng thái
    scheduler = AuctionScheduler.getInstance();
    scheduler.resetForTesting();

    // 3. Bơm (Inject) các mock object vào
    scheduler.init(mockBiddingService, mockServer);

    // 4. Chuẩn bị dữ liệu trung tâm trên RAM
    auctionManager = AuctionManager.getInstance();
    auctionManager.clearCacheForTesting(); // SỬA TẠI ĐÂY: Dùng hàm dọn dẹp chuẩn

    Item item = new Electronics("item1", "Test Item", "Desc", 12);
    Seller seller = new Seller("seller1", "seller", "pass");

    // Tạo một phiên đấu giá giả (Đã hết hạn)
    testAuction = new Auction("auc_test_1", item, seller, 1000.0,
        LocalDateTime.now().minusHours(2),
        LocalDateTime.now().minusHours(1));

    // Giả lập người chiến thắng
    Bidder winner = new Bidder("bidder1", "alice", "pass");
    testAuction.setHighestBidder(winner);
    testAuction.setCurrentPrice(1500.0);

    // Thêm vào hệ thống (Lệnh này sẽ nới rộng tác vụ ghi xuống DB)
    auctionManager.addAuction(testAuction);

    // SỬA TẠI ĐÂY: Ép Hàng đợi ghi xong phiên mồi này rồi mới bắt đầu bài test
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();
  }

  @AfterEach
  void tearDown() {
    // SỬA TẠI ĐÂY: Dùng resetForTesting để dọn dẹp triệt để (đã bao gồm stop())
    scheduler.resetForTesting();
  }

  /**
   * Hàm tiện ích dùng kỹ thuật Reflection để gọi một hàm private trong Java.
   * Tránh việc phải dùng Thread.sleep() chờ đợi hệ thống tự gọi gây chậm bài test.
   */
  private void invokePrivateScanMethod() throws Exception {
    Method method = AuctionScheduler.class.getDeclaredMethod("scanExpiredAuctions");
    method.setAccessible(true); // "Phá khóa" bảo vệ private
    method.invoke(scheduler);   // Thực thi hàm
  }

  @Test
  @DisplayName("Phát sóng thông báo khi quét thấy có phiên đấu giá vừa hết hạn")
  void testScanExpiredAuctions_HasExpiredAuction() throws Exception {
    // 1. KỊCH BẢN (Mocking Behavior):
    // Dặn dò BiddingService giả rằng: Nếu có ai hỏi kiểm tra id "auc_test_1",
    // hãy trả về đối tượng testAuction (báo hiệu là nó VỪA BỊ ĐÓNG).
    when(mockBiddingService.checkAndCloseIfExpired("auc_test_1")).thenReturn(testAuction);

    // 2. THỰC THI: Gọi trực tiếp vòng quét ngầm
    invokePrivateScanMethod();

    // 3. KIỂM CHỨNG:
    // Đảm bảo rằng Scheduler đã gọi lệnh phát sóng (broadcast) của máy chủ
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockServer, times(1)).broadcastMessage(messageCaptor.capture());

    String broadcastMessage = messageCaptor.getValue();

    // Kiểm tra xem chuỗi JSON sinh ra có chứa đúng các thông tin quan trọng không
    assertTrue(broadcastMessage.contains("AUCTION_FINISHED"), "Hành động phải là AUCTION_FINISHED");
    assertTrue(broadcastMessage.contains("auc_test_1"), "Phải chứa ID của phiên");
    assertTrue(broadcastMessage.contains("alice"), "Phải nhắc tên người chiến thắng");
    assertTrue(broadcastMessage.contains("1500.0"), "Phải chứa giá chung cuộc");
  }

  @Test
  @DisplayName("Không phát sóng gì nếu không có phiên đấu giá nào hết hạn")
  void testScanExpiredAuctions_NoExpiredAuction() throws Exception {
    // 1. KỊCH BẢN:
    // Lần này, BiddingService trả về null (báo hiệu phiên vẫn đang chạy, hoặc đã đóng từ lâu)
    when(mockBiddingService.checkAndCloseIfExpired("auc_test_1")).thenReturn(null);

    // 2. THỰC THI
    invokePrivateScanMethod();

    // 3. KIỂM CHỨNG:
    // Tuyệt đối KHÔNG ĐƯỢC gọi hàm phát sóng của máy chủ (never)
    verify(mockServer, never()).broadcastMessage(anyString());
  }
}