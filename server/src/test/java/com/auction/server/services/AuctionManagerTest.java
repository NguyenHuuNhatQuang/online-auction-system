package com.auction.server.services;

import com.auction.common.models.Auction;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;
import com.auction.common.models.Electronics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử lớp quản lý trung tâm AuctionManager.
 */
class AuctionManagerTest {

  private AuctionManager auctionManager;

  @BeforeEach
  void setUp() {
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