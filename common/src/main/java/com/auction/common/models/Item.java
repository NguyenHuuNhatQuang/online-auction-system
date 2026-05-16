package com.auction.common.models;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm được đưa lên sàn đấu giá.
 * Kế thừa từ {@link Entity}.
 */
public abstract class Item extends Entity {

  private String name;
  private String description;

  /**
   * Khởi tạo thông tin cơ bản của một sản phẩm.
   *
   * @param id          Định danh sản phẩm.
   * @param name        Tên sản phẩm.
   * @param description Mô tả chi tiết về sản phẩm.
   */
  public Item(String id, String name, String description) {
    super(id);
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Xác định loại của sản phẩm (ví dụ: Electronics, Art).
   *
   * @return Chuỗi đại diện cho phân loại sản phẩm.
   */
  public abstract String getItemType();
}