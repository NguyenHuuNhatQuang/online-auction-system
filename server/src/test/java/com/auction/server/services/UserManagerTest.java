package com.auction.server.services;

import com.auction.common.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử logic quản lý và xác thực người dùng.
 */
class UserManagerTest {

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
}