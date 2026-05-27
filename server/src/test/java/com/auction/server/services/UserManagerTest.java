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
  void testAuthenticate_Success_Bidder() {
    UserManager userManager = UserManager.getInstance();
    // 1. Tạo user mồi
    userManager.register("alice", "123", "BIDDER");
    // 2. ÉP ĐỢI: Chờ ghi xong vào DB
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();

    // 3. Test đăng nhập
    User user = userManager.authenticate("alice", "123");
    assertNotNull(user, "Phải trả về đối tượng User khi đăng nhập đúng");
  }

  @Test
  void testAuthenticate_Success_Seller() {
    UserManager userManager = UserManager.getInstance();
    userManager.register("bob", "123", "SELLER");
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();

    User user = userManager.authenticate("bob", "123");
    assertNotNull(user);
  }

  @Test
  void testRegister_Success() {
    UserManager userManager = UserManager.getInstance();
    User newUser = userManager.register("new_bidder_test", "123", "BIDDER");
    assertNotNull(newUser);

    // ÉP ĐỢI: Chờ DB ghi xong rồi mới test đăng nhập
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();

    User loggedInUser = userManager.authenticate("new_bidder_test", "123");
    assertNotNull(loggedInUser, "Phải đăng nhập được ngay bằng tài khoản vừa đăng ký");
  }

  @Test
  void testRegister_Fail_DuplicateUsername() {
    UserManager userManager = UserManager.getInstance();
    userManager.register("duplicate_user", "123", "SELLER");

    // ÉP ĐỢI: Chờ user 1 ghi xong thì hàm check trùng mới có tác dụng
    com.auction.server.database.DatabaseWriteQueue.getInstance().flushForTesting();

    User failedUser = userManager.register("duplicate_user", "456", "BIDDER");
    assertNull(failedUser, "Phải trả về null khi cố tình đăng ký trùng tên");
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
}