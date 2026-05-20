package com.auction.server.services;

import com.auction.common.models.Bidder;
import com.auction.common.models.Seller;
import com.auction.common.models.User;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách người dùng.
 * Hiện tại đang dùng dữ liệu ảo (Mock) trên RAM.
 * Sau này chỉ cần đổi ruột class này thành gọi Database (JDBC) là xong.
 */
public class UserManager {

  private static volatile UserManager instance;
  private final ConcurrentHashMap<String, User> users;

  private UserManager() {
    users = new ConcurrentHashMap<>();
    // Tạo sẵn dữ liệu ảo để test (Mật khẩu chung là 123)
    // Alice là Bidder (chỉ được mua)
    users.put("alice", new Bidder("u1", "alice", "123"));
    // Bob là Seller (được quyền tạo phiên đấu giá)
    users.put("bob", new Seller("u2", "bob", "123"));
  }

  public static UserManager getInstance() {
    if (instance == null) {
      synchronized (UserManager.class) {
        if (instance == null) {
          instance = new UserManager();
        }
      }
    }
    return instance;
  }

  /**
   * Xác thực đăng nhập.
   * @return Đối tượng User nếu đúng tài khoản/mật khẩu, ngược lại trả về null.
   */
  public User authenticate(String username, String password) {
    User user = users.get(username);
    if (user != null && user.getPassword().equals(password)) {
      return user;
    }
    return null;
  }

  public User register(String username, String password, String role) {
    if (users.containsKey(username)) return null; // Trùng tên
    User newUser = "SELLER".equalsIgnoreCase(role)
        ? new Seller("u" + (users.size() + 1), username, password)
        : new Bidder("u" + (users.size() + 1), username, password);
    users.put(username, newUser);
    return newUser;
  }
}