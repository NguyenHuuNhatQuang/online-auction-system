package com.auction.server.services;

import com.auction.common.models.Electronics;
import com.auction.common.models.Item;
import com.auction.server.database.DatabaseConnection;
import com.auction.server.database.DatabaseWriteQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử kho quản lý sản phẩm độc lập.
 */
class ItemManagerTest {

  private ItemManager itemManager;
  private String testSellerId;

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

    itemManager = ItemManager.getInstance();
    testSellerId = "seller_" + UUID.randomUUID().toString(); // Dùng ID ngẫu nhiên để tránh xung đột giữa các test
  }

  @Test
  @DisplayName("Thêm và lấy sản phẩm từ kho thành công")
  void testAddAndGetItem() {
    Item item = new Electronics("item_test_1", "TV Sony", "4K", 24);
    itemManager.addItem(item, testSellerId);

    DatabaseWriteQueue.getInstance().flushForTesting();

    Item retrieved = itemManager.getItem("item_test_1");
    assertNotNull(retrieved, "Phải tìm thấy sản phẩm vừa thêm");
    assertEquals("TV Sony", retrieved.getName());
  }

  @Test
  @DisplayName("Lấy danh sách sản phẩm theo đúng người bán (Seller)")
  void testGetItemsBySeller() {
    Item item1 = new Electronics("item_test_2", "Laptop", "Gaming", 12);
    Item item2 = new Electronics("item_test_3", "Mouse", "Wireless", 6);

    itemManager.addItem(item1, testSellerId);
    itemManager.addItem(item2, testSellerId);
    // Thêm 1 sản phẩm của người khác
    itemManager.addItem(new Electronics("item_other", "Bàn phím", "Cơ", 12), "other_seller");

    DatabaseWriteQueue.getInstance().flushForTesting();

    Collection<Item> sellerItems = itemManager.getItemsBySeller(testSellerId);

    assertEquals(2, sellerItems.size(), "Chỉ được lấy đúng 2 sản phẩm của testSellerId");
  }

  @Test
  @DisplayName("Xóa sản phẩm khỏi kho thành công")
  void testDeleteItem() {
    Item item = new Electronics("item_test_4", "Headphone", "Bluetooth", 12);
    itemManager.addItem(item, testSellerId);
    itemManager.deleteItem("item_test_4");

    DatabaseWriteQueue.getInstance().flushForTesting();

    assertNull(itemManager.getItem("item_test_4"), "Sản phẩm phải bị xóa khỏi kho");
    assertTrue(itemManager.getItemsBySeller(testSellerId).isEmpty(), "Danh sách của người bán phải trống");
  }
}