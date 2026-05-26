package com.auction.server.services;

import com.auction.common.models.Auction;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;
import com.auction.common.models.Electronics;
import com.auction.server.database.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử lớp quản lý trung tâm AuctionManager.
 */
class AuctionManagerTest {

  private AuctionManager auctionManager;

  @BeforeEach
  void setUp() {
    // 1. Chờ hàng đợi chạy xong mọi thứ thừa thãi từ test trước để nhả lock file
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();

    // 2. Dọn sạch bộ nhớ đệm trên RAM
    com.auction.server.services.AuctionManager.getInstance().clearCacheForTesting();

    try {
      // 1. Xóa file database cũ nếu tồn tại
      Files.deleteIfExists(Paths.get("auction_system.db"));

      // 2. Tạo lại file và cấu trúc bảng mới tinh
      DatabaseConnection.initDatabase();

    } catch (Exception e) {
      System.err.println("Lỗi dọn dẹp database trước khi test: " + e.getMessage());
    }

    auctionManager = AuctionManager.getInstance();
    // Xóa sạch dữ liệu cũ trong bộ nhớ (nếu có) do đặc thù Singleton lưu state toàn cục
    auctionManager.getAllAuctions().clear();
  }

  @Test
  @DisplayName("Kiểm tra tính chất Singleton: Chỉ trả về một instance duy nhất")
  void testSingletonInstance() {
    AuctionManager instance1 = AuctionManager.getInstance();
    AuctionManager instance2 = AuctionManager.getInstance();

    assertSame(instance1, instance2, "Cả hai tham chiếu phải trỏ về cùng một vùng nhớ");
  }

  @Test
  @DisplayName("Thêm và lấy phiên đấu giá thành công")
  void testAddAndGetAuction() {
    Item item = new Electronics("item1", "Test", "Desc", 12);
    Seller seller = new Seller("seller1", "user", "pass");
    Auction auction = new Auction("auc1", item, seller, 100, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

    auctionManager.addAuction(auction);
    Auction retrieved = auctionManager.getAuction("auc1");

    assertNotNull(retrieved, "Phải tìm thấy phiên đấu giá vừa thêm");
    assertEquals("auc1", retrieved.getId(), "ID phải khớp nhau");
    assertEquals(1, auctionManager.getAllAuctions().size(), "Danh sách phải có 1 phần tử");
  }
}