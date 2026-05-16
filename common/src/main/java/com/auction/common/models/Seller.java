package com.auction.common.models;

/**
 * Đại diện cho người bán (Seller).
 * Có quyền đăng bán, quản lý thông tin sản phẩm đấu giá.
 */
public class Seller extends User {

  /**
   * Khởi tạo một Seller mới. Vai trò mặc định là "SELLER".
   *
   * @param id       Định danh Seller.
   * @param username Tên đăng nhập.
   * @param password Mật khẩu.
   */
  public Seller(String id, String username, String password) {
    super(id, username, password, "SELLER");
  }

  @Override
  public void printInfo() {
    System.out.println("[Seller Profile] Username: " + getUsername() + ", ID: " + getId());
  }
}