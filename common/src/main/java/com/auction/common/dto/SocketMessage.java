package com.auction.common.dto;

/**
 * Đối tượng truyền tải dữ liệu (DTO) chuẩn hóa cho mọi giao tiếp Socket.
 * Mọi chuỗi JSON gửi qua mạng đều phải parse được thành đối tượng này.
 */
public class SocketMessage {

  private String action;
  private String payload;

  // Jackson yêu cầu phải có constructor rỗng (mặc định)
  public SocketMessage() {}

  /**
   * Khởi tạo một thông điệp giao tiếp mạng.
   *
   * @param action  Hành động (VD: "PLACE_BID", "BID_SUCCESS", "ERROR").
   * @param payload Dữ liệu đính kèm dưới dạng chuỗi JSON (VD: {"auctionId":"123", "amount":100}).
   */
  public SocketMessage(String action, String payload) {
    this.action = action;
    this.payload = payload;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}