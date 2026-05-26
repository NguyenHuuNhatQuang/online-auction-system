package com.auction.server.database;

import com.auction.common.models.Auction;
import com.auction.common.models.Bidder;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

public class AuctionDAO {

  private final ItemDAO itemDAO = new ItemDAO();

  public void insertAuction(Auction auction) throws SQLException {
    String sql = "INSERT INTO auctions (id, item_id, seller_id, start_price, current_price, highest_bidder_id, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, auction.getId());
      pstmt.setString(2, auction.getItem().getId());
      pstmt.setString(3, auction.getSeller().getId());
      pstmt.setDouble(4, auction.getStartPrice());
      pstmt.setDouble(5, auction.getCurrentPrice());
      pstmt.setString(6, auction.getHighestBidder() != null ? auction.getHighestBidder().getId() : null);
      pstmt.setString(7, auction.getStartTime().toString());
      pstmt.setString(8, auction.getEndTime().toString());
      pstmt.setString(9, auction.getStatus());
      pstmt.executeUpdate();
    }
  }

  public void updateAuctionPriceAndBidder(String auctionId, double currentPrice, String bidderId) throws SQLException {
    String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setDouble(1, currentPrice);
      pstmt.setString(2, bidderId);
      pstmt.setString(3, auctionId);
      pstmt.executeUpdate();
    }
  }

  public void updateAuctionStatus(String auctionId, String status) throws SQLException {
    String sql = "UPDATE auctions SET status = ? WHERE id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, status);
      pstmt.setString(2, auctionId);
      pstmt.executeUpdate();
    }
  }

  public Collection<Auction> getAllAuctions() {
    Collection<Auction> list = new ArrayList<>();
    String sql = "SELECT a.id, a.item_id, a.seller_id, a.start_price, a.current_price, a.highest_bidder_id, a.start_time, a.end_time, a.status, "
        + "u1.username AS seller_name, u1.password AS seller_pass, "
        + "u2.username AS bidder_name, u2.password AS bidder_pass "
        + "FROM auctions a "
        + "JOIN users u1 ON a.seller_id = u1.id "
        + "LEFT JOIN users u2 ON a.highest_bidder_id = u2.id";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        String id = rs.getString("id");
        double startPrice = rs.getDouble("start_price");
        double currentPrice = rs.getDouble("current_price");
        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
        String status = rs.getString("status");

        Item item = itemDAO.getItem(rs.getString("item_id"));
        if (item == null) continue; // Bỏ qua nếu dữ liệu lỗi liên kết

        Seller seller = new Seller(rs.getString("seller_id"), rs.getString("seller_name"), rs.getString("seller_pass"));

        Auction auction = new Auction(id, item, seller, startPrice, startTime, endTime);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(status);

        String bidderId = rs.getString("highest_bidder_id");
        if (bidderId != null) {
          Bidder bidder = new Bidder(bidderId, rs.getString("bidder_name"), rs.getString("bidder_pass"));
          auction.setHighestBidder(bidder);
        }
        list.add(auction);
      }
    } catch (SQLException e) {
      System.err.println("[AuctionDAO] Lỗi phục hồi danh sách đấu giá: " + e.getMessage());
    }
    return list;
  }
}