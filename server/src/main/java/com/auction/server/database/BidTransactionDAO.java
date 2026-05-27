package com.auction.server.database;

import com.auction.common.models.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BidTransactionDAO {

  public void insertBid(BidTransaction bid) throws SQLException {
    String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, bid.getId());
      pstmt.setString(2, bid.getAuctionId());
      pstmt.setString(3, bid.getBidder().getId());
      pstmt.setDouble(4, bid.getBidAmount());
      pstmt.setString(5, bid.getTimestamp().toString());
      pstmt.executeUpdate();
    }
  }

  public java.util.List<BidTransaction> getBidsByAuction(String auctionId) {
    java.util.List<BidTransaction> history = new java.util.ArrayList<>();

    // Nối bảng với Users để lấy thông tin username của người đặt giá
    String sql = "SELECT b.id, b.auction_id, b.bid_amount, b.timestamp, " +
        "u.id AS user_id, u.username, u.password, u.role " +
        "FROM bid_transactions b " +
        "JOIN users u ON b.bidder_id = u.id " +
        "WHERE b.auction_id = ? " +
        "ORDER BY b.timestamp ASC"; // Sắp xếp cũ nhất lên trước

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, auctionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          String txId = rs.getString("id");
          double amount = rs.getDouble("bid_amount");
          java.time.LocalDateTime timestamp = java.time.LocalDateTime.parse(rs.getString("timestamp"));

          // Tạo lại đối tượng Bidder
          com.auction.common.models.Bidder bidder = new com.auction.common.models.Bidder(
              rs.getString("user_id"),
              rs.getString("username"),
              rs.getString("password")
          );

          BidTransaction tx = new BidTransaction(txId, auctionId, bidder, amount, timestamp);
          history.add(tx);
        }
      }
    } catch (SQLException e) {
      System.err.println("[BidTransactionDAO] Lỗi lấy lịch sử đấu giá: " + e.getMessage());
    }

    return history;
  }
}