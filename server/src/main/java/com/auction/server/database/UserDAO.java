package com.auction.server.database;

import com.auction.common.models.Bidder;
import com.auction.common.models.Seller;
import com.auction.common.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object cho bảng Users.
 */
public class UserDAO {

  // Tác vụ ĐỌC (Nhanh, không làm khóa DB, chạy trực tiếp trên luồng của Client)
  public User getUserByUsername(String username) {
    String sql = "SELECT id, username, password, role FROM users WHERE username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String id = rs.getString("id");
        String pass = rs.getString("password");
        String role = rs.getString("role");

        if ("SELLER".equalsIgnoreCase(role)) {
          return new Seller(id, username, pass);
        } else {
          return new Bidder(id, username, pass);
        }
      }
    } catch (SQLException e) {
      System.err.println("[UserDAO] Lỗi truy xuất User: " + e.getMessage());
    }
    return null; // Không tìm thấy
  }

  // Tác vụ GHI (Sẽ được gọi thông qua DatabaseWriteQueue)
  public void insertUser(User user) throws SQLException {
    String sql = "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, user.getId());
      pstmt.setString(2, user.getUsername());
      pstmt.setString(3, user.getPassword());
      pstmt.setString(4, user.getRole());
      pstmt.executeUpdate();
    }
  }
}