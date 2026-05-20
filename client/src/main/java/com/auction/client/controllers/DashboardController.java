package com.auction.client.controllers;

import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DashboardController {

  @FXML private Label welcomeLabel;
  @FXML private ListView<String> auctionListView;

  @FXML private HBox createAuctionInputBox;
  @FXML private Button createAuctionBtn;

  private NetworkClient networkClient;
  private String currentUser;
  private final ObjectMapper objectMapper = new ObjectMapper();


  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    String userRole = SceneManager.getInstance().getUserRole();
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    welcomeLabel.setText("Sảnh Chờ Đấu Giá - Xin chào " + currentUser + " (" + userRole + ")");
    networkClient.setOnMessageReceived(this::handleServerMessage);

    if ("BIDDER".equalsIgnoreCase(userRole)) {
      createAuctionBtn.setVisible(false);
      createAuctionBtn.setManaged(false);
    }

    networkClient.sendMessage("{\"action\":\"GET_ACTIVE_AUCTIONS\", \"payload\":\"\"}");
  }

  @FXML
  private void handleGoToManageProducts() {
    SceneManager.getInstance().switchScene("/fxml/product_management.fxml", "Kho hàng của tôi - " + currentUser);
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
            case "ACTIVE_AUCTIONS_LIST": {
              JsonNode arrayNode = objectMapper.readTree(message.getPayload());
              auctionListView.getItems().clear();
              for (JsonNode node : arrayNode) {
                String aucId = node.get("auctionId").asText();
                String itemName = node.get("itemName").asText();
                double price = node.get("currentPrice").asDouble();
                String top = node.get("highestBidder").asText();
                String status = node.get("status").asText();
                String statusText = "[ĐANG CHẠY]";

                if ("FINISHED".equals(status)) {
                  statusText = "[KẾT THÚC]";
                } else if ("PAID".equals(status)) {
                  statusText = "[ĐÃ THANH TOÁN]";
                }

                auctionListView.getItems().add(String.format("%s %s | %s | Top: %s | Giá: $%.2f", statusText, aucId, itemName, top, price));
              }
              break;
            }

            case "NEW_AUCTION_BROADCAST": {
              JsonNode node = objectMapper.readTree(message.getPayload());
              String aucId = node.get("auctionId").asText();
              String itemName = node.get("itemName").asText();
              double price = node.get("currentPrice").asDouble();

              String displayText = String.format("[ĐANG CHẠY] %s | %s | Top: Chưa có | Giá: $%.2f", aucId, itemName, price);
              if (!auctionListView.getItems().contains(displayText)) {
                auctionListView.getItems().add(displayText);
              }
              break;
            }

            case "NEW_BID_BROADCAST": {
              JsonNode bidNode = objectMapper.readTree(message.getPayload());
              String targetAuctionId = bidNode.get("auctionId").asText();
              double newPrice = bidNode.get("newPrice").asDouble();
              String highestBidder = bidNode.get("highestBidder").asText();

              for (int i = 0; i < auctionListView.getItems().size(); i++) {
                String line = auctionListView.getItems().get(i);
                if (line.contains(targetAuctionId)) {
                  // Giữ lại phần đầu (Trạng thái + ID + Tên), chỉ thay đổi Top và Giá
                  String[] parts = line.split("\\|");
                  if (parts.length >= 3) {
                    parts[2] = String.format(" Top: %s ", highestBidder);
                    parts[3] = String.format(" Giá: $%.2f", newPrice);
                    auctionListView.getItems().set(i, String.join("|", parts));
                  }
                  break;
                }
              }
              break;
            }

            case "AUCTION_FINISHED": {
              JsonNode endNode = objectMapper.readTree(message.getPayload());
              String endedId = endNode.get("auctionId").asText();
              String winner = endNode.get("winner").asText();
              double finalPrice = endNode.get("finalPrice").asDouble();

              for (int i = 0; i < auctionListView.getItems().size(); i++) {
                String line = auctionListView.getItems().get(i);
                if (line.contains(endedId)) {
                  // Đổi mác thành KẾT THÚC và chốt người thắng
                  String updatedLine = line.replace("[ĐANG CHẠY]", "[KẾT THÚC]");
                  String[] parts = updatedLine.split("\\|");
                  if (parts.length >= 3) {
                    parts[2] = String.format(" Top: %s (WIN) ", winner);
                    parts[3] = String.format(" Giá Chốt: $%.2f", finalPrice);
                    auctionListView.getItems().set(i, String.join("|", parts));
                  }
                  break;
                }
              }
              break;
            }

            case "AUCTION_PAID": {
              JsonNode paidNode = objectMapper.readTree(message.getPayload());
              String paidId = paidNode.get("auctionId").asText();
              for (int i = 0; i < auctionListView.getItems().size(); i++) {
                String line = auctionListView.getItems().get(i);
                if (line.contains(paidId)) {
                  auctionListView.getItems().set(i, line.replace("[KẾT THÚC]", "[ĐÃ THANH TOÁN]"));
                  break;
                }
              }
              break;
            }

            case "SERVER_DISCONNECTED": {
              showAlert("Mất kết nối", "Máy chủ đã dừng hoạt động. Vui lòng thoát ứng dụng.");
              break;
            }

            case "ERROR": {
              showAlert("Lỗi hệ thống", message.getPayload());
              break;
            }
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
  private void handleJoinAuction() {
    String selectedItem = auctionListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) return;

    String[] parts = selectedItem.split("\\|");
    String auctionId = parts[0].replaceAll("\\[.*?\\]", "").trim(); // Lọc bỏ chữ [ĐANG CHẠY] hoặc [KẾT THÚC]

    // Nếu phiên đã thanh toán
    if (selectedItem.startsWith("[ĐÃ THANH TOÁN]")) {
      showAlert("Thông báo", "Sản phẩm này đã được thanh toán xong.");
      return;
    }

    // Nếu phiên kết thúc nhưng chưa thanh toán
    if (selectedItem.startsWith("[KẾT THÚC]")) {
      // SỬA TẠI ĐÂY: Chỉ cần kiểm tra xem tên mình có nằm ở phần Top hay không
      if (selectedItem.contains("Top: " + currentUser)) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn là người thắng! Bạn có muốn thanh toán phiên này ngay không?");
        confirm.showAndWait().ifPresent(response -> {
          if (response == javafx.scene.control.ButtonType.OK) {
            String request = String.format("{\"action\":\"PAY_AUCTION\", \"payload\":\"{\\\"auctionId\\\":\\\"%s\\\"}\"}", auctionId);
            networkClient.sendMessage(request);
          }
        });
      } else {
        showAlert("Từ chối", "Phiên đấu giá đã kết thúc. Bạn không phải là người chiến thắng.");
      }
      return;
    }

    // Nếu đang chạy thì vào phòng bình thường
    SceneManager.getInstance().setCurrentAuctionId(auctionId);
    SceneManager.getInstance().switchScene("/fxml/auction_room.fxml", "Phòng Đấu Giá - " + auctionId);
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