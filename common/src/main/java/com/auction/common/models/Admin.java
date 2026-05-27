package com.auction.common.models;

/**
 * Đại diện cho Quản trị viên (Admin) của hệ thống.
 * Có quyền quản lý toàn bộ người dùng, sản phẩm và phiên đấu giá.
 */
public class Admin extends User {

  public Admin(String id, String username, String password) {
    super(id, username, password, "ADMIN");
  }

  @Override
  public void printInfo() {
    System.out.println("[Admin Profile] Username: " + getUsername() + ", ID: " + getId());
  }
}