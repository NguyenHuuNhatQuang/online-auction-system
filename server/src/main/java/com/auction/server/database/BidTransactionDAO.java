package com.auction.server.database;

import com.auction.common.models.BidTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
}