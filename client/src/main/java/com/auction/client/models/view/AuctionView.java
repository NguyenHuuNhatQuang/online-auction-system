package com.auction.client.models.view;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Mô hình hiển thị (view-model) cho một phiên đấu giá trên màn hình "Sảnh chờ".
 *
 * <p>Đây là lớp cơ sở dùng chung. Phần "skin" khác nhau theo loại sản phẩm
 * (icon, dải màu gradient, nhãn danh mục) được tách ra cho các lớp con
 * {@link ElectronicsAuctionView} và {@link ArtAuctionView} đảm nhận — nhờ đó
 * không phải đụng vào logic cũ mà vẫn mở rộng được.</p>
 */
public class AuctionView {

  protected final String id;
  protected final String name;
  protected final String description;
  protected final double price;
  protected final String topBidder;
  protected final String status;     // RUNNING | FINISHED | PAID | CANCELED
  protected final String itemType;   // ELECTRONICS | ART | null
  protected final String sellerName;
  protected final int bidCount;
  protected final LocalDateTime endTime;

  protected AuctionView(String id, String name, String description, double price,
                        String topBidder, String status, String itemType,
                        String sellerName, int bidCount, LocalDateTime endTime) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
    this.topBidder = topBidder;
    this.status = status;
    this.itemType = itemType;
    this.sellerName = sellerName;
    this.bidCount = bidCount;
    this.endTime = endTime;
  }

  /**
   * Nhà máy: chọn lớp con phù hợp dựa trên itemType trả về từ server.
   */
  public static AuctionView fromJson(JsonNode node) {
    String id = text(node, "auctionId", "");
    String name = text(node, "itemName", "");
    String desc = text(node, "itemDesc", "");
    double price = node.has("currentPrice") ? node.get("currentPrice").asDouble() : 0;
    String top = text(node, "highestBidder", "Chưa có");
    String status = text(node, "status", "RUNNING");
    String type = node.has("itemType") && !node.get("itemType").isNull() ? node.get("itemType").asText() : null;
    String seller = text(node, "sellerName", "—");
    int bids = node.has("bidCount") ? node.get("bidCount").asInt() : 0;
    LocalDateTime end = parseTime(text(node, "endTime", null));

    if (type != null && type.equalsIgnoreCase("ELECTRONICS")) {
      return new ElectronicsAuctionView(id, name, desc, price, top, status, type, seller, bids, end);
    } else if (type != null && type.equalsIgnoreCase("ART")) {
      return new ArtAuctionView(id, name, desc, price, top, status, type, seller, bids, end);
    }
    return new AuctionView(id, name, desc, price, top, status, type, seller, bids, end);
  }

  private static String text(JsonNode node, String field, String fallback) {
    return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : fallback;
  }

  private static LocalDateTime parseTime(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return LocalDateTime.parse(raw);
    } catch (Exception e) {
      return null;
    }
  }

  // ---------------- Phần "skin" — lớp con sẽ override ----------------

  /** Biểu tượng đại diện cho thẻ sản phẩm. */
  public String getIcon() {
    return "📦";
  }

  /** Style-class của vùng ảnh (dải gradient). */
  public String getPicStyleClass() {
    return "pic-generic";
  }

  /** Nhãn danh mục hiển thị trên thẻ. */
  public String getCategoryLabel() {
    return (itemType != null) ? itemType : "KHÁC";
  }

  // ---------------- Trạng thái / định dạng dùng chung ----------------

  public boolean isEnded() {
    return "FINISHED".equals(status) || "PAID".equals(status) || "CANCELED".equals(status);
  }

  public String getStatusText() {
    switch (status) {
      case "FINISHED": return "FINISHED";
      case "PAID": return "PAID";
      case "CANCELED": return "CANCELED";
      default: return "LIVE";
    }
  }

  /** Style-class badge khớp với app.css (badge-live / badge-finished / badge-paid). */
  public String getStatusBadgeClass() {
    switch (status) {
      case "FINISHED": return "badge-finished";
      case "PAID": return "badge-paid";
      case "CANCELED": return "badge-canceled";
      default: return "badge-live";
    }
  }

  public String getPriceLabel() {
    return isEnded() ? "Giá chốt" : "Giá cao nhất";
  }

  public String getFormattedPrice() {
    return String.format("%,.0f đ", price);
  }

  /** Dòng phụ: seller + số lượt bid (hoặc thông tin người thắng khi đã kết thúc). */
  public String getSubInfo() {
    if (isEnded()) {
      return "Top: " + topBidder + " · " + bidCount + " lượt";
    }
    return "Seller: " + sellerName + " · " + bidCount + " lượt bid";
  }

  /** Chuỗi đếm ngược tính theo thời điểm hiện tại; rỗng nếu đã kết thúc. */
  public String getCountdownText(LocalDateTime now) {
    if (isEnded() || endTime == null) return "";
    Duration d = Duration.between(now, endTime);
    if (d.isNegative() || d.isZero()) return "⏱ 00:00";
    long total = d.getSeconds();
    long h = total / 3600;
    long m = (total % 3600) / 60;
    long s = total % 60;
    if (h > 0) {
      return String.format("⏱ %02d:%02d:%02d", h, m, s);
    }
    return String.format("⏱ %02d:%02d", m, s);
  }

  // ---------------- Getter cơ bản ----------------

  public String getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public double getPrice() { return price; }
  public String getTopBidder() { return topBidder; }
  public String getStatus() { return status; }
  public String getItemType() { return itemType; }
  public String getSellerName() { return sellerName; }
  public int getBidCount() { return bidCount; }
  public LocalDateTime getEndTime() { return endTime; }
}
