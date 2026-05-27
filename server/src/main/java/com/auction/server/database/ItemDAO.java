package com.auction.server.database;

import com.auction.common.models.Art;
import com.auction.common.models.Electronics;
import com.auction.common.models.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Thao tác dữ liệu SQL với bảng items.
 */
public class ItemDAO {

  public void insertItem(Item item, String sellerId) throws SQLException {
    String sql = "INSERT INTO items (id, name, description, type, warranty_months, artist, seller_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, item.getId());
      pstmt.setString(2, item.getName());
      pstmt.setString(3, item.getDescription());
      pstmt.setString(4, item.getItemType());

      if (item instanceof Electronics) {
        pstmt.setInt(5, ((Electronics) item).getWarrantyMonths());
        pstmt.setString(6, "Unknown");
      } else if (item instanceof Art) {
        pstmt.setInt(5, 0);
        pstmt.setString(6, ((Art) item).getArtist());
      }
      pstmt.setString(7, sellerId);
      pstmt.executeUpdate();
    }
  }

  public Item getItem(String itemId) {
    String sql = "SELECT id, name, description, type, warranty_months, artist FROM items WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, itemId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          String id = rs.getString("id");
          String name = rs.getString("name");
          String desc = rs.getString("description");
          String type = rs.getString("type");

          if ("ELECTRONICS".equalsIgnoreCase(type)) {
            return new Electronics(id, name, desc, rs.getInt("warranty_months"));
          } else if ("ART".equalsIgnoreCase(type)) {
            return new Art(id, name, desc, rs.getString("artist"));
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("[ItemDAO] Lỗi lấy thông tin sản phẩm: " + e.getMessage());
    }
    return null;
  }

  public void deleteItem(String itemId) throws SQLException {
    String sql = "DELETE FROM items WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, itemId);
      pstmt.executeUpdate();
    }
  }

  public void updateItem(String itemId, String name, String description) throws SQLException {
    String sql = "UPDATE items SET name = ?, description = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, name);
      pstmt.setString(2, description);
      pstmt.setString(3, itemId);
      pstmt.executeUpdate();
    }
  }

  public Collection<Item> getItemsBySeller(String sellerId) {
    Collection<Item> list = new ArrayList<>();
    String sql = "SELECT id, name, description, type, warranty_months, artist FROM items WHERE seller_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sellerId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString("id");
          String name = rs.getString("name");
          String desc = rs.getString("description");
          String type = rs.getString("type");

          if ("ELECTRONICS".equalsIgnoreCase(type)) {
            list.add(new Electronics(id, name, desc, rs.getInt("warranty_months")));
          } else if ("ART".equalsIgnoreCase(type)) {
            list.add(new Art(id, name, desc, rs.getString("artist")));
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("[ItemDAO] Lỗi lấy kho sản phẩm của Seller: " + e.getMessage());
    }
    return list;
  }

  public Collection<Item> getAllItems() {
    Collection<Item> list = new ArrayList<>();
    String sql = "SELECT id, name, description, type, warranty_months, artist, seller_id FROM items";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        String type = rs.getString("type");
        String sellerId = rs.getString("seller_id"); // Lấy thêm thông tin chủ sở hữu

        Item item;
        if ("ELECTRONICS".equalsIgnoreCase(type)) {
          item = new Electronics(id, name, desc, rs.getInt("warranty_months"));
        } else {
          item = new Art(id, name, desc, rs.getString("artist"));
        }
        // Gắn tạm tên người bán vào description để Admin dễ nhìn
        item.setDescription(desc + " (Owner: " + sellerId + ")");
        list.add(item);
      }
    } catch (SQLException e) {
      System.err.println("[ItemDAO] Lỗi lấy toàn bộ kho sản phẩm: " + e.getMessage());
    }
    return list;
  }
}