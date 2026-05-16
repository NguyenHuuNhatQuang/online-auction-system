package com.auction.common.models;

/**
 * Lớp trừu tượng đại diện cho một người dùng trong hệ thống đấu giá.
 * Kế thừa từ {@link Entity}.
 */
public abstract class User extends Entity {

  private String username;
  private String password;
  private String role;

  /**
   * Khởi tạo đối tượng người dùng.
   *
   * @param id       Định danh người dùng.
   * @param username Tên đăng nhập.
   * @param password Mật khẩu (đã được mã hóa ở thực tế).
   * @param role     Vai trò của người dùng (VD: BIDDER, SELLER, ADMIN).
   */
  public User(String id, String username, String password, String role) {
    super(id);
    this.username = username;
    this.password = password;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  /**
   * Phương thức trừu tượng in thông tin chi tiết của người dùng.
   * Các lớp con bắt buộc phải ghi đè (override) phương thức này.
   */
  public abstract void printInfo();
}