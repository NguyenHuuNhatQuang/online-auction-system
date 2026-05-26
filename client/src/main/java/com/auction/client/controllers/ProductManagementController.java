package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.util.List;

public class ProductManagementController {

  @FXML protected ListView<String> itemListView;
  @FXML private TextField nameField;
  @FXML private TextField descField;
  @FXML private ComboBox<String> typeComboBox;
  @FXML private TextField attrField;
  @FXML private TextField startPriceField;
  @FXML private TextField durationField;

  private NetworkClient networkClient;
  private String currentUser;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<String> itemIds = new ArrayList<>();

  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    this.networkClient = SceneManager.getInstance().getNetworkClient();

    typeComboBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART"));
    typeComboBox.getSelectionModel().selectFirst();

    networkClient.setOnMessageReceived(this::handleServerMessage);
    refreshInventory();
  }

  private void refreshInventory() {
    String request = String.format("{\"action\":\"GET_SELLER_ITEMS\", \"payload\":\"{\\\"sellerId\\\":\\\"%s\\\"}\"}", currentUser);
    networkClient.sendMessage(request);
  }

  private void handleServerMessage(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);
      Platform.runLater(() -> {
        try {
          if ("SELLER_ITEMS_LIST".equals(message.getAction())) {
            JsonNode arrayNode = objectMapper.readTree(message.getPayload());
            itemListView.getItems().clear();
            itemIds.clear();
            List<InvItem> inventory = new ArrayList<>();

            for (JsonNode node : arrayNode) {
              String id = node.get("id").asText();
              String name = node.get("name").asText();
              String type = node.get("type").asText();
              String desc = node.get("desc").asText();

              String extra = "";
              String attr = "";
              if (node.has("warrantyMonths")) {
                extra = " [Bảo hành: " + node.get("warrantyMonths").asText() + " tháng]";
                attr = "Bảo hành " + node.get("warrantyMonths").asText() + " tháng";
              } else if (node.has("artist")) {
                extra = " [Họa sĩ: " + node.get("artist").asText() + "]";
                attr = "Tác giả: " + node.get("artist").asText();
              }

              itemListView.getItems().add(String.format("(%s) %s - %s %s", type, name, desc, extra));
              itemIds.add(id);
              inventory.add(new InvItem(id, name, type, desc, attr));
            }
            // Điểm mở rộng: lớp con có thể override để vẽ kho dạng lưới thẻ.
            renderInventory(inventory);
          } else if ("AUCTION_CREATED".equals(message.getAction())) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã được mở phiên đấu giá realtime công cộng công khai!");
            startPriceField.clear();
            durationField.clear();
          } else if ("ERROR".equals(message.getAction())) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", message.getPayload());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi phân tích JSON tại trang quản lý kho: " + e.getMessage());
    }
  }

  @FXML
  private void handleAddItem() {
    String name = nameField.getText().trim();
    String desc = descField.getText().trim();
    String type = typeComboBox.getValue();
    String attr = attrField.getText().trim();

    if (name.isEmpty() || desc.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập tên và mô tả sản phẩm!");
      return;
    }

    String payload;
    if ("ELECTRONICS".equals(type)) {
      int warranty = 0;
      if (!attr.isEmpty()) {
        try {
          warranty = Integer.parseInt(attr);
          // THÊM KIỂM TRA SỐ ÂM:
          if (warranty < 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tháng bảo hành không được là số âm!");
            return;
          }
        } catch (NumberFormatException e) {
          showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tháng bảo hành phải là số nguyên hợp lệ!");
          return;
        }
      }
      payload = String.format("{\\\"itemType\\\":\\\"ELECTRONICS\\\", \\\"itemName\\\":\\\"%s\\\", \\\"itemDesc\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\", \\\"warrantyMonths\\\":%d}", name, desc, currentUser, warranty);
    } else {
      String artist = attr.isEmpty() ? "Unknown" : attr;
      payload = String.format("{\\\"itemType\\\":\\\"ART\\\", \\\"itemName\\\":\\\"%s\\\", \\\"itemDesc\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\", \\\"artist\\\":\\\"%s\\\"}", name, desc, currentUser, artist);
    }

    networkClient.sendMessage(String.format("{\"action\":\"ADD_ITEM\", \"payload\":\"%s\"}", payload));
    nameField.clear();
    descField.clear();
    attrField.clear();
  }

  @FXML
  private void handleLaunchAuction() {
    int selectedIndex = itemListView.getSelectionModel().getSelectedIndex();
    if (selectedIndex < 0) {
      showAlert(Alert.AlertType.WARNING, "Chưa lựa chọn", "Vui lòng chọn một sản phẩm trong danh sách kho!");
      return;
    }

    String itemId = itemIds.get(selectedIndex);
    String priceText = startPriceField.getText().trim();
    String durationText = durationField.getText().trim();

    if (priceText.isEmpty() || durationText.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Cần cung cấp giá khởi điểm và thời lượng phiên!");
      return;
    }

    try {
      double startPrice = Double.parseDouble(priceText);
      int duration = Integer.parseInt(durationText);

      // THÊM KIỂM TRA SỐ ÂM & BẰNG 0:
      if (startPrice < 0) {
        showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm không được là số âm!");
        return;
      }
      if (duration <= 0) {
        showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Thời gian đấu giá phải lớn hơn 0 phút!");
        return;
      }

      String payload = String.format(
          "{\\\"itemId\\\":\\\"%s\\\", \\\"startPrice\\\":%s, \\\"durationMinutes\\\":%d, \\\"sellerId\\\":\\\"%s\\\", \\\"sellerName\\\":\\\"%s\\\"}",
          itemId, startPrice, duration, currentUser, currentUser
      );
      networkClient.sendMessage(String.format("{\"action\":\"CREATE_AUCTION\", \"payload\":\"%s\"}", payload));
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Sai định dạng", "Giá tiền và số phút phải là chữ số hợp lệ.");
    }
  }

  @FXML
  private void handleEditItem() {
    int selectedIndex = itemListView.getSelectionModel().getSelectedIndex();
    if (selectedIndex < 0) {
      showAlert(Alert.AlertType.WARNING, "Lỗi", "Chọn một sản phẩm để sửa!");
      return;
    }
    String newName = nameField.getText().trim();
    String newDesc = descField.getText().trim();
    if (newName.isEmpty() || newDesc.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Nhập Tên và Mô tả mới vào ô bên trên để cập nhật!");
      return;
    }
    String itemId = itemIds.get(selectedIndex);
    String payload = String.format("{\\\"itemId\\\":\\\"%s\\\", \\\"newName\\\":\\\"%s\\\", \\\"newDesc\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\"}", itemId, newName, newDesc, currentUser);
    networkClient.sendMessage(String.format("{\"action\":\"UPDATE_ITEM\", \"payload\":\"%s\"}", payload));
    nameField.clear(); descField.clear();
  }

  @FXML
  private void handleDeleteItem() {
    int selectedIndex = itemListView.getSelectionModel().getSelectedIndex();
    if (selectedIndex < 0) return;

    String itemId = itemIds.get(selectedIndex);
    networkClient.sendMessage(String.format("{\"action\":\"DELETE_ITEM\", \"payload\":\"{\\\"itemId\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\"}\"}", itemId, currentUser));
  }

  @FXML
  private void handleBack() {
    SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + currentUser);
  }

  /**
   * Một dòng sản phẩm trong kho (dữ liệu thuần để hiển thị).
   * Thứ tự của danh sách trùng khớp với chỉ số trong {@code itemListView}/{@code itemIds}.
   */
  protected static class InvItem {
    public final String id;
    public final String name;
    public final String type;   // ELECTRONICS | ART
    public final String desc;
    public final String attr;   // "Bảo hành 12 tháng" / "Tác giả: ..." (có thể rỗng)

    InvItem(String id, String name, String type, String desc, String attr) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.desc = desc;
      this.attr = attr;
    }
  }

  /**
   * Điểm mở rộng: được gọi mỗi khi kho được làm tươi. Mặc định không làm gì
   * (giữ nguyên hành vi ListView cũ). Lớp con override để vẽ lưới thẻ.
   */
  protected void renderInventory(List<InvItem> items) {
    // no-op
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}