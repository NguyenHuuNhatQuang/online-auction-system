package com.auction.common.models;

/**
 * Đại diện cho sản phẩm đồ điện tử.
 * Bổ sung thêm thuộc tính thời gian bảo hành so với sản phẩm thông thường.
 */
public class Electronics extends Item {

  private int warrantyMonths;

  /**
   * Khởi tạo sản phẩm đồ điện tử.
   *
   * @param id             Định danh sản phẩm.
   * @param name           Tên sản phẩm.
   * @param description    Mô tả chi tiết.
   * @param warrantyMonths Số tháng bảo hành.
   */
  public Electronics(String id, String name, String description, int warrantyMonths) {
    super(id, name, description);
    this.warrantyMonths = warrantyMonths;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public String getItemType() {
    return "ELECTRONICS";
  }
}