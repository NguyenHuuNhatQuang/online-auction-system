package com.auction.common.models;

import java.time.LocalDateTime;

/**
 * Quản lý trung tâm của một phiên đấu giá.
 * Lưu trữ trạng thái, thời gian và giá trị hiện tại của phiên.
 */
public class Auction extends Entity {

  private Item item;
  private Seller seller;
  private double startPrice;

  // Các biến có thể thay đổi liên tục bởi nhiều luồng (Concurrency)
  private volatile double currentPrice;
  private volatile Bidder highestBidder;
  private volatile String status; // OPEN, RUNNING, FINISHED, PAID, CANCELED

  private LocalDateTime startTime;
  private LocalDateTime endTime;

  /**
   * Khởi tạo một phiên đấu giá mới. Trạng thái mặc định là "OPEN".
   *
   * @param id         Định danh phiên đấu giá.
   * @param item       Sản phẩm đấu giá.
   * @param seller     Người bán.
   * @param startPrice Giá khởi điểm.
   * @param startTime  Thời gian bắt đầu.
   * @param endTime    Thời gian kết thúc.
   */
  public Auction(String id, Item item, Seller seller, double startPrice,
                 LocalDateTime startTime, LocalDateTime endTime) {
    super(id);
    this.item = item;
    this.seller = seller;
    this.startPrice = startPrice;
    this.currentPrice = startPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = "OPEN";
  }

  // --- Getters cho dữ liệu tĩnh ---
  public Item getItem() {
    return item;
  }

  public Seller getSeller() {
    return seller;
  }

  public double getStartPrice() {
    return startPrice;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  // --- Getters / Setters cho dữ liệu động (Cần Thread-safe) ---

  /**
   * Lấy giá hiện tại cao nhất. Sử dụng synchronized để đảm bảo Thread-safe.
   */
  public synchronized double getCurrentPrice() {
    return currentPrice;
  }

  public synchronized void setCurrentPrice(double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public synchronized Bidder getHighestBidder() {
    return highestBidder;
  }

  public synchronized void setHighestBidder(Bidder highestBidder) {
    this.highestBidder = highestBidder;
  }

  public synchronized String getStatus() {
    return status;
  }

  public synchronized void setStatus(String status) {
    this.status = status;
  }
}