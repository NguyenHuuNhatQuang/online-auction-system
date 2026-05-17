package com.auction.client.controllers;

import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DashboardController {

  @FXML private Label welcomeLabel;
  @FXML private ListView<String> auctionListView;

  private NetworkClient networkClient;
  private String currentUser;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    welcomeLabel.setText("Sảnh Chờ Đấu Giá - Xin chào " + currentUser);
    networkClient.setOnMessageReceived(this::handleServerMessage);

    // THÊM DÒNG NÀY: Vừa mở màn hình là hỏi Server ngay danh sách hiện tại
    networkClient.sendMessage("{\"action\":\"GET_ACTIVE_AUCTIONS\", \"payload\":\"\"}");
  }

  /**
   * Xử lý tin nhắn từ Server trả về (Được gọi từ luồng mạng ngầm)
   */
  private void handleServerMessage(String jsonMessage) {
    System.out.println("[Dashboard] Nhận thông điệp: " + jsonMessage);

    try {
      // Phân tích JSON thành đối tượng SocketMessage
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);

      // Bất kỳ thao tác nào làm thay đổi giao diện đều PHẢI nằm trong Platform.runLater
      Platform.runLater(() -> {
        try {
          switch (message.getAction()) {
            case "ACTIVE_AUCTIONS_LIST":
              // Quét mảng JSON Server trả về và in ra ListView
              JsonNode arrayNode = objectMapper.readTree(message.getPayload());
              auctionListView.getItems().clear(); // Xóa trắng danh sách cũ
              for (JsonNode node : arrayNode) {
                auctionListView.getItems().add("Phiên đấu giá: " + node.get("auctionId").asText());
              }
              break;

            case "NEW_AUCTION_BROADCAST":
            case "AUCTION_CREATED":
              // Bóc tách payload bên trong để lấy Auction ID
              JsonNode payloadNode = objectMapper.readTree(message.getPayload());
              String auctionId = payloadNode.get("auctionId").asText();

              // Thêm vào danh sách hiển thị
              String displayText = "Phiên đấu giá: " + auctionId;
              if (!auctionListView.getItems().contains(displayText)) {
                auctionListView.getItems().add(displayText);
              }
              break;

            case "SERVER_DISCONNECTED":
              showAlert("Mất kết nối", "Máy chủ đã dừng hoạt động. Vui lòng thoát ứng dụng.");
              break;

            case "ERROR":
              showAlert("Lỗi hệ thống", message.getPayload());
              break;
          }
        } catch (Exception e) {
          System.err.println("Lỗi xử lý payload: " + e.getMessage());
        }
      });

    } catch (Exception e) {
      System.err.println("Không thể parse JSON: " + jsonMessage);
    }
  }

  @FXML
  private void handleCreateAuction() {
    // Gửi lệnh tạo phiên đấu giá mẫu (Giả định bán một cái Laptop)
    String payload = String.format(
        "{\"itemType\":\"ELECTRONICS\", \"itemName\":\"Laptop Gaming\", \"itemDesc\":\"Mới 100%%\", \"startPrice\":1000.0, \"durationMinutes\":10, \"sellerId\":\"%s\", \"sellerName\":\"%s\"}",
        currentUser, currentUser
    );

    String requestJson = String.format("{\"action\":\"CREATE_AUCTION\", \"payload\":%s}", escapeJson(payload));
    networkClient.sendMessage(requestJson);
  }

  @FXML
  private void handleJoinAuction() {
    String selectedItem = auctionListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Lỗi", "Vui lòng chọn một phiên đấu giá trong danh sách để tham gia!");
      return;
    }

    // Chuỗi trong list có dạng: "Phiên đấu giá: AUC_12345"
    // Ta cắt lấy phần sau dấu hai chấm và xóa khoảng trắng để lấy đúng ID
    String[] parts = selectedItem.split(":");
    if (parts.length == 2) {
      String auctionId = parts[1].trim();

      // Lưu ID vào SceneManager và chuyển trang
      SceneManager.getInstance().setCurrentAuctionId(auctionId);
      SceneManager.getInstance().switchScene("/fxml/auction_room.fxml", "Phòng Đấu Giá - " + auctionId);
    }
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  // Hàm phụ trợ bọc JSON payload thành chuỗi hợp lệ
  private String escapeJson(String raw) {
    return "\"" + raw.replace("\"", "\\\"") + "\"";
  }
}