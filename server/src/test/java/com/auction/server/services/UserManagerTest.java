package com.auction.server.services;

import com.auction.common.models.User;
import com.auction.server.database.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử logic quản lý và xác thực người dùng.
 */
class UserManagerTest {
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
  }

  @Test
  @DisplayName("Đăng nhập thành công với tài khoản hợp lệ (BIDDER)")
  void testAuthenticate_Success_Bidder() {
    UserManager userManager = UserManager.getInstance();
    User user = userManager.authenticate("alice", "123");

    assertNotNull(user, "Phải trả về đối tượng User khi đăng nhập đúng");
    assertEquals("alice", user.getUsername(), "Tên người dùng phải khớp");
    assertEquals("BIDDER", user.getRole(), "Alice phải có quyền BIDDER");
  }

  @Test
  @DisplayName("Đăng nhập thành công với tài khoản hợp lệ (SELLER)")
  void testAuthenticate_Success_Seller() {
    UserManager userManager = UserManager.getInstance();
    User user = userManager.authenticate("bob", "123");

    assertNotNull(user);
    assertEquals("SELLER", user.getRole(), "Bob phải có quyền SELLER");
  }

  @Test
  @DisplayName("Đăng nhập thất bại khi sai mật khẩu hoặc tài khoản không tồn tại")
  void testAuthenticate_Fail() {
    UserManager userManager = UserManager.getInstance();

    // Sai mật khẩu
    User user1 = userManager.authenticate("alice", "wrong_password");
    assertNull(user1, "Phải trả về null khi sai mật khẩu");

    // Tài khoản không tồn tại
    User user2 = userManager.authenticate("hacker", "123");
    assertNull(user2, "Phải trả về null khi tài khoản không tồn tại");
  }

  @Test
  @DisplayName("Đăng ký tài khoản mới thành công")
  void testRegister_Success() {
    UserManager userManager = UserManager.getInstance();

    // Đăng ký một tài khoản mới tinh
    User newUser = userManager.register("new_bidder_test", "123", "BIDDER");

    assertNotNull(newUser, "Phải trả về đối tượng User sau khi đăng ký");
    assertEquals("new_bidder_test", newUser.getUsername(), "Tên người dùng phải khớp");
    assertEquals("BIDDER", newUser.getRole(), "Vai trò phải được gán đúng là BIDDER");

    // Đăng nhập thử bằng tài khoản vừa tạo
    User loggedInUser = userManager.authenticate("new_bidder_test", "123");
    assertNotNull(loggedInUser, "Phải đăng nhập được ngay bằng tài khoản vừa đăng ký");
  }

  @Test
  @DisplayName("Đăng ký thất bại khi tên đăng nhập đã tồn tại")
  void testRegister_Fail_DuplicateUsername() {
    UserManager userManager = UserManager.getInstance();

    // Đăng ký lần 1
    userManager.register("duplicate_user", "123", "SELLER");

    // Đăng ký lần 2 với cùng tên
    User failedUser = userManager.register("duplicate_user", "456", "BIDDER");

    assertNull(failedUser, "Phải trả về null khi cố tình đăng ký trùng tên");
  }
}