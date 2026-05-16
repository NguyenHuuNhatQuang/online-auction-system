package com.auction.common.models;

/**
 * Đại diện cho người tham gia đấu giá (Bidder).
 * Có quyền xem sản phẩm và đặt giá.
 */
public class Bidder extends User {

  /**
   * Khởi tạo một Bidder mới. Vai trò mặc định là "BIDDER".
   *
   * @param id       Định danh Bidder.
   * @param username Tên đăng nhập.
   * @param password Mật khẩu.
   */
  public Bidder(String id, String username, String password) {
    super(id, username, password, "BIDDER");
  }

  @Override
  public void printInfo() {
    System.out.println("[Bidder Profile] Username: " + getUsername() + ", ID: " + getId());
  }
}