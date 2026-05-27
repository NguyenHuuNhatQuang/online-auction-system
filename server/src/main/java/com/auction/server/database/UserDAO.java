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
        } else if ("ADMIN".equalsIgnoreCase(role)) {
          return new com.auction.common.models.Admin(id, username, pass); // THÊM DÒNG NÀY
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

  // Lấy toàn bộ danh sách người dùng
  public java.util.List<User> getAllUsers() {
    java.util.List<User> users = new java.util.ArrayList<>();
    String sql = "SELECT id, username, password, role FROM users";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String pass = rs.getString("password");
        String role = rs.getString("role");

        if ("SELLER".equalsIgnoreCase(role)) {
          users.add(new Seller(id, username, pass));
        } else if ("ADMIN".equalsIgnoreCase(role)) {
          users.add(new com.auction.common.models.Admin(id, username, pass));
        } else {
          users.add(new Bidder(id, username, pass));
        }
      }
    } catch (SQLException e) {
      System.err.println("[UserDAO] Lỗi lấy danh sách User: " + e.getMessage());
    }
    return users;
  }

  // Cập nhật vai trò (Role)
  public void updateUserRole(String username, String newRole) throws SQLException {
    String sql = "UPDATE users SET role = ? WHERE username = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, newRole);
      pstmt.setString(2, username);
      pstmt.executeUpdate();
    }
  }
}