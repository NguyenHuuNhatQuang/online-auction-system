package com.auction.common.models;

/**
 * Lớp cơ sở (Base Class) cho tất cả các đối tượng có tính định danh trong hệ thống.
 * Cung cấp thuộc tính ID chung để dễ dàng quản lý và truy xuất từ cơ sở dữ liệu.
 */
public abstract class Entity {

  /** Định danh duy nhất của thực thể (thường dùng UUID). */
  protected String id;

  /**
   * Khởi tạo một thực thể mới với ID xác định.
   *
   * @param id Chuỗi định danh duy nhất.
   */
  public Entity(String id) {
    this.id = id;
  }

  /**
   * Lấy ID của thực thể.
   *
   * @return Chuỗi ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Cập nhật ID cho thực thể.
   *
   * @param id Chuỗi ID mới.
   */
  public void setId(String id) {
    this.id = id;
  }
}