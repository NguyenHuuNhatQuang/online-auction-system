package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

/**
 * Wizard 4 bước mở phiên đấu giá (S13-15, S25).
 * Tái sử dụng đúng giao thức cũ: ADD_ITEM -> SELLER_ITEMS_LIST (diff để tìm id mới) -> CREATE_AUCTION.
 */
public class ListingWizardController {

  @FXML private VBox stepBox1, stepBox2, stepBox3, stepBox4;
  @FXML private VBox pane1, pane2, pane3, pane4;
  @FXML private VBox electronicsCard, artCard;
  @FXML private Label attrLabel;
  @FXML private TextField wzNameField, wzDescField, wzAttrField;
  @FXML private TextField wzPriceField, wzDurationField;
  @FXML private Label reviewType, reviewName, reviewDesc, reviewAttrLabel, reviewAttr, reviewPrice, reviewDuration;
  @FXML private Button btnBack, btnNext, btnSubmit;

  private NetworkClient networkClient;
  private String currentUser;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private int currentStep = 1;
  private String selectedType = "ELECTRONICS";

  private final Set<String> baselineIds = new HashSet<>();
  private boolean baselineReady = false;
  private boolean awaitingNewItem = false;

  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    networkClient.setOnMessageReceived(this::handleServerMessage);

    selectType("ELECTRONICS");
    showStep(1);

    // Chụp ảnh kho hiện tại để sau ADD_ITEM có thể diff ra id mới
    networkClient.sendMessage(String.format(
        "{\"action\":\"GET_SELLER_ITEMS\", \"payload\":\"{\\\"sellerId\\\":\\\"%s\\\"}\"}", currentUser));
  }

  // ---------------- Type selection ----------------

  @FXML private void selectElectronics() { selectType("ELECTRONICS"); }
  @FXML private void selectArt() { selectType("ART"); }

  private void selectType(String type) {
    this.selectedType = type;
    electronicsCard.getStyleClass().remove("type-card-selected");
    artCard.getStyleClass().remove("type-card-selected");
    if ("ART".equals(type)) {
      artCard.getStyleClass().add("type-card-selected");
      attrLabel.setText("TÁC GIẢ");
      wzAttrField.setPromptText("Da Vinci");
    } else {
      electronicsCard.getStyleClass().add("type-card-selected");
      attrLabel.setText("BẢO HÀNH (THÁNG)");
      wzAttrField.setPromptText("12");
    }
  }

  // ---------------- Navigation ----------------

  @FXML
  private void handleNext() {
    if (!validateStep(currentStep)) return;
    if (currentStep == 3) populateReview();
    if (currentStep < 4) showStep(currentStep + 1);
  }

  @FXML
  private void handlePrev() {
    if (currentStep > 1) showStep(currentStep - 1);
  }

  private void showStep(int step) {
    this.currentStep = step;

    pane1.setVisible(step == 1); pane1.setManaged(step == 1);
    pane2.setVisible(step == 2); pane2.setManaged(step == 2);
    pane3.setVisible(step == 3); pane3.setManaged(step == 3);
    pane4.setVisible(step == 4); pane4.setManaged(step == 4);

    styleStep(stepBox1, 1, step);
    styleStep(stepBox2, 2, step);
    styleStep(stepBox3, 3, step);
    styleStep(stepBox4, 4, step);

    btnBack.setVisible(step > 1); btnBack.setManaged(step > 1);
    btnNext.setVisible(step < 4); btnNext.setManaged(step < 4);
    btnSubmit.setVisible(step == 4); btnSubmit.setManaged(step == 4);
  }

  private void styleStep(VBox box, int index, int current) {
    box.getStyleClass().removeAll("wizard-step-active", "wizard-step-done");
    if (index == current) {
      box.getStyleClass().add("wizard-step-active");
    } else if (index < current) {
      box.getStyleClass().add("wizard-step-done");
    }
  }

  private boolean validateStep(int step) {
    switch (step) {
      case 2:
        if (wzNameField.getText().trim().isEmpty() || wzDescField.getText().trim().isEmpty()) {
          showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập tên và mô tả sản phẩm!");
          return false;
        }
        if ("ELECTRONICS".equals(selectedType) && !wzAttrField.getText().trim().isEmpty()) {
          try {
            if (Integer.parseInt(wzAttrField.getText().trim()) < 0) {
              showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tháng bảo hành không được là số âm!");
              return false;
            }
          } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tháng bảo hành phải là số nguyên hợp lệ!");
            return false;
          }
        }
        return true;
      case 3:
        try {
          double price = Double.parseDouble(wzPriceField.getText().trim());
          int duration = Integer.parseInt(wzDurationField.getText().trim());
          if (price < 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm không được là số âm!");
            return false;
          }
          if (duration <= 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Thời gian đấu giá phải lớn hơn 0 phút!");
            return false;
          }
        } catch (NumberFormatException e) {
          showAlert(Alert.AlertType.ERROR, "Sai định dạng", "Giá tiền và số phút phải là chữ số hợp lệ.");
          return false;
        }
        return true;
      default:
        return true;
    }
  }

  private void populateReview() {
    reviewType.setText(selectedType);
    reviewName.setText(wzNameField.getText().trim());
    reviewDesc.setText(wzDescField.getText().trim());
    String attr = wzAttrField.getText().trim();
    if ("ART".equals(selectedType)) {
      reviewAttrLabel.setText("Tác giả:");
      reviewAttr.setText(attr.isEmpty() ? "Unknown" : attr);
    } else {
      reviewAttrLabel.setText("Bảo hành:");
      reviewAttr.setText((attr.isEmpty() ? "0" : attr) + " tháng");
    }
    reviewPrice.setText(wzPriceField.getText().trim() + " VND");
    reviewDuration.setText(wzDurationField.getText().trim() + " phút");
  }

  // ---------------- Submit ----------------

  @FXML
  private void handleSubmit() {
    if (!baselineReady) {
      showAlert(Alert.AlertType.WARNING, "Đang đồng bộ", "Đang tải kho sản phẩm, vui lòng thử lại sau giây lát.");
      return;
    }
    String name = wzNameField.getText().trim();
    String desc = wzDescField.getText().trim();
    String attr = wzAttrField.getText().trim();

    String payload;
    if ("ELECTRONICS".equals(selectedType)) {
      int warranty = attr.isEmpty() ? 0 : Integer.parseInt(attr);
      payload = String.format(
          "{\\\"itemType\\\":\\\"ELECTRONICS\\\", \\\"itemName\\\":\\\"%s\\\", \\\"itemDesc\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\", \\\"warrantyMonths\\\":%d}",
          name, desc, currentUser, warranty);
    } else {
      String artist = attr.isEmpty() ? "Unknown" : attr;
      payload = String.format(
          "{\\\"itemType\\\":\\\"ART\\\", \\\"itemName\\\":\\\"%s\\\", \\\"itemDesc\\\":\\\"%s\\\", \\\"sellerId\\\":\\\"%s\\\", \\\"artist\\\":\\\"%s\\\"}",
          name, desc, currentUser, artist);
    }

    awaitingNewItem = true;
    btnSubmit.setDisable(true);
    btnSubmit.setText("Đang xử lý...");
    networkClient.sendMessage(String.format("{\"action\":\"ADD_ITEM\", \"payload\":\"%s\"}", payload));
  }

  private void launchAuction(String itemId) {
    String price = wzPriceField.getText().trim();
    int duration = Integer.parseInt(wzDurationField.getText().trim());
    String payload = String.format(
        "{\\\"itemId\\\":\\\"%s\\\", \\\"startPrice\\\":%s, \\\"durationMinutes\\\":%d, \\\"sellerId\\\":\\\"%s\\\", \\\"sellerName\\\":\\\"%s\\\"}",
        itemId, price, duration, currentUser, currentUser);
    networkClient.sendMessage(String.format("{\"action\":\"CREATE_AUCTION\", \"payload\":\"%s\"}", payload));
  }

  // ---------------- Server messages ----------------

  private void handleServerMessage(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);
      Platform.runLater(() -> {
        try {
          switch (message.getAction()) {
            case "SELLER_ITEMS_LIST": {
              JsonNode arrayNode = objectMapper.readTree(message.getPayload());
              if (!awaitingNewItem) {
                baselineIds.clear();
                for (JsonNode node : arrayNode) baselineIds.add(node.get("id").asText());
                baselineReady = true;
              } else {
                String newId = null;
                String lastId = null;
                for (JsonNode node : arrayNode) {
                  String id = node.get("id").asText();
                  lastId = id;
                  if (!baselineIds.contains(id)) newId = id;
                }
                String target = (newId != null) ? newId : lastId;
                awaitingNewItem = false;
                if (target != null) {
                  launchAuction(target);
                } else {
                  resetSubmit();
                  showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được sản phẩm vừa thêm.");
                }
              }
              break;
            }
            case "AUCTION_CREATED": {
              showAlert(Alert.AlertType.INFORMATION, "Thành công",
                  "Phiên đấu giá đã được mở công khai realtime!");
              SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + currentUser);
              break;
            }
            case "ERROR": {
              resetSubmit();
              awaitingNewItem = false;
              showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", message.getPayload());
              break;
            }
          }
        } catch (Exception e) {
          System.err.println("Lỗi xử lý wizard: " + e.getMessage());
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi parse JSON wizard: " + e.getMessage());
    }
  }

  private void resetSubmit() {
    btnSubmit.setDisable(false);
    btnSubmit.setText("🚀 Mở Phiên");
  }

  @FXML
  private void handleCancel() {
    SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + currentUser);
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
