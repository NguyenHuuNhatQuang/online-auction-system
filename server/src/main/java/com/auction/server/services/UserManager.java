package com.auction.server.services;

import com.auction.common.models.Bidder;
import com.auction.common.models.Seller;
import com.auction.common.models.User;
import com.auction.server.database.DatabaseWriteQueue;
import com.auction.server.database.UserDAO;

import java.util.UUID;

public class UserManager {

  private static volatile UserManager instance;
  private final UserDAO userDAO;

  private UserManager() {
    this.userDAO = new UserDAO();
  }

  public static UserManager getInstance() {
    if (instance == null) {
      synchronized (UserManager.class) {
        if (instance == null) instance = new UserManager();
      }
    }
    return instance;
  }

  public User authenticate(String username, String password) {
    // Đọc trực tiếp từ Database
    User user = userDAO.getUserByUsername(username);
    if (user != null && user.getPassword().equals(password)) {
      return user;
    }
    return null;
  }

  public User register(String username, String password, String role) {
    if (userDAO.getUserByUsername(username) != null) {
      return null;
    }

    String newId = username;

    User newUser = "SELLER".equalsIgnoreCase(role)
        ? new Seller(newId, username, password)
        : new Bidder(newId, username, password);

    // Đẩy lệnh lưu xuống Hàng đợi Database
    DatabaseWriteQueue.getInstance().execute(() -> {
      try {
        userDAO.insertUser(newUser);
        System.out.println("[UserManager] Đã ghi User mới vào Database: " + username);
      } catch (Exception e) {
        System.err.println("[UserManager] Lỗi lưu User vào DB: " + e.getMessage());
      }
    });

    return newUser;
  }
}