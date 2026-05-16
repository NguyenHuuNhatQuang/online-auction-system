package com.auction.common.models;

import java.time.LocalDateTime;

/**
 * Ghi nhận một giao dịch đặt giá thành công.
 * Dùng để lưu vết lịch sử đấu giá (Audit/History).
 */
public class BidTransaction extends Entity {

  private String auctionId;
  private Bidder bidder;
  private double bidAmount;
  private LocalDateTime timestamp;

  /**
   * Khởi tạo giao dịch lịch sử.
   *
   * @param id         Định danh giao dịch.
   * @param auctionId  ID của phiên đấu giá liên quan.
   * @param bidder     Người đã đặt giá.
   * @param bidAmount  Số tiền đã đặt.
   * @param timestamp  Thời điểm ghi nhận hệ thống.
   */
  public BidTransaction(String id, String auctionId, Bidder bidder,
                        double bidAmount, LocalDateTime timestamp) {
    super(id);
    this.auctionId = auctionId;
    this.bidder = bidder;
    this.bidAmount = bidAmount;
    this.timestamp = timestamp;
  }

  public String getAuctionId() { return auctionId; }
  public Bidder getBidder() { return bidder; }
  public double getBidAmount() { return bidAmount; }
  public LocalDateTime getTimestamp() { return timestamp; }
}