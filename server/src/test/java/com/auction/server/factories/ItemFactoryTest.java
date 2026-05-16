package com.auction.server.factories;

import com.auction.common.models.Art;
import com.auction.common.models.Electronics;
import com.auction.common.models.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử cho ItemFactory.
 * Đảm bảo Factory khởi tạo đúng loại đối tượng và ném ngoại lệ khi đầu vào sai.
 */
class ItemFactoryTest {

  @Test
  @DisplayName("Tạo thành công sản phẩm loại Electronics với thuộc tính đặc thù")
  void testCreateElectronics_Success() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("warrantyMonths", 24);

    Item item = ItemFactory.createItem("ELECTRONICS", "Laptop Dell", "Core i7", attributes);

    assertNotNull(item.getId(), "ID không được để trống");
    assertTrue(item instanceof Electronics, "Đối tượng trả về phải là instance của Electronics");
    assertEquals("ELECTRONICS", item.getItemType(), "Loại sản phẩm phải là ELECTRONICS");

    // Ép kiểu để kiểm tra thuộc tính riêng
    Electronics electronics = (Electronics) item;
    assertEquals(24, electronics.getWarrantyMonths(), "Số tháng bảo hành phải được gán đúng");
  }

  @Test
  @DisplayName("Tạo thành công sản phẩm loại Art với giá trị mặc định nếu thiếu thuộc tính")
  void testCreateArt_Success_DefaultAttributes() {
    // Truyền map rỗng để kiểm tra xem hệ thống có gán giá trị mặc định (Unknown) không
    Map<String, Object> emptyAttributes = new HashMap<>();

    Item item = ItemFactory.createItem("ART", "Mona Lisa", "Bản sao", emptyAttributes);

    assertTrue(item instanceof Art, "Đối tượng trả về phải là instance của Art");
    Art art = (Art) item;
    assertEquals("Unknown", art.getArtist(), "Tên tác giả phải là Unknown khi không truyền vào");
  }

  @Test
  @DisplayName("Ném ngoại lệ khi yêu cầu tạo loại sản phẩm không hỗ trợ")
  void testCreateItem_Fail_InvalidType() {
    Map<String, Object> attributes = new HashMap<>();

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ItemFactory.createItem("VEHICLE", "VinFast", "VF8", attributes);
    });

    assertTrue(exception.getMessage().contains("không hợp lệ hoặc chưa được hỗ trợ"));
  }
}