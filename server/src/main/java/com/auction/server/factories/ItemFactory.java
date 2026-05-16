package com.auction.server.factories;

import com.auction.common.models.Art;
import com.auction.common.models.Electronics;
import com.auction.common.models.Item;

import java.util.Map;
import java.util.UUID;

/**
 * Lớp Factory áp dụng mẫu thiết kế Factory Method.
 * Chịu trách nhiệm khởi tạo các đối tượng Item cụ thể dựa trên loại (type) được yêu cầu.
 * Giúp giấu đi logic khởi tạo phức tạp và dễ dàng mở rộng khi có thêm loại sản phẩm mới.
 */
public class ItemFactory {

  /**
   * Tạo một đối tượng Item mới dựa trên các tham số đầu vào.
   *
   * @param type        Loại sản phẩm (ví dụ: "ELECTRONICS", "ART").
   * @param name        Tên sản phẩm.
   * @param description Mô tả chi tiết.
   * @param attributes  Bản đồ (Map) chứa các thuộc tính đặc thù của từng loại sản phẩm.
   * @return Đối tượng Item đã được khởi tạo.
   * @throws IllegalArgumentException Nếu loại sản phẩm không được hỗ trợ.
   */
  public static Item createItem(String type, String name, String description, Map<String, Object> attributes) {
    String id = UUID.randomUUID().toString();

    switch (type.toUpperCase()) {
      case "ELECTRONICS":
        // Mặc định bảo hành 0 tháng nếu không truyền vào
        int warranty = (int) attributes.getOrDefault("warrantyMonths", 0);
        return new Electronics(id, name, description, warranty);

      case "ART":
        // Mặc định tác giả là Unknown nếu không truyền vào
        String artist = (String) attributes.getOrDefault("artist", "Unknown");
        return new Art(id, name, description, artist);

      default:
        throw new IllegalArgumentException("Loại sản phẩm không hợp lệ hoặc chưa được hỗ trợ: " + type);
    }
  }
}