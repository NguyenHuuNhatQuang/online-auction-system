package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AuctionRoomController {

  @FXML private Label roomTitleLabel;
  @FXML private Label currentPriceLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea logArea;

  private NetworkClient networkClient;
  private String currentUser;
  private String auctionId;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    this.auctionId = SceneManager.getInstance().getCurrentAuctionId();
    this.networkClient = SceneManager.getInstance().getNetworkClient();

    roomTitleLabel.setText("Phiên Đấu Giá: " + auctionId);
    logArea.appendText("Bạn đã vào phòng. Đang đồng bộ dữ liệu...\n");

    networkClient.setOnMessageReceived(this::handleServerMessage);

    String payload = "{\"auctionId\":\"" + auctionId + "\"}";
    String request = "{\"action\":\"GET_AUCTION_STATE\", \"payload\":\"" + payload.replace("\"", "\\\"") + "\"}";
    networkClient.sendMessage(request);
  }

  private void handleServerMessage(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);

      Platform.runLater(() -> {
        try {
          switch (message.getAction()) {
            case "AUCTION_FINISHED":
              JsonNode endNode = objectMapper.readTree(message.getPayload());
              String endedAuctionId = endNode.get("auctionId").asText();

              // Nếu thông báo này dành cho phòng đang mở
              if (this.auctionId.equals(endedAuctionId)) {
                String winner = endNode.get("winner").asText();
                double finalPrice = endNode.get("finalPrice").asDouble();

                logArea.appendText("\n==============================\n");
                logArea.appendText("BÚA ĐÃ GÕ! PHIÊN ĐẤU GIÁ KẾT THÚC.\n");
                if ("Không có ai".equals(winner)) {
                  logArea.appendText("Sản phẩm không có ai mua.\n");
                } else {
                  logArea.appendText("Người chiến thắng: " + winner + " với giá $" + finalPrice + "\n");
                }
                logArea.appendText("==============================\n");

                // Khóa UI không cho đặt giá nữa
                bidAmountField.setDisable(true);
              }
              break;

            case "AUCTION_STATE":
              JsonNode stateNode = objectMapper.readTree(message.getPayload());
              double currentPrice = stateNode.get("currentPrice").asDouble();
              String currentBidder = stateNode.get("highestBidder").asText();
              String status = stateNode.get("status").asText();

              currentPriceLabel.setText(String.format("Giá cao nhất: $%.2f", currentPrice));

              if ("CLOSED".equals(status)) {
                logArea.appendText(">> Phiên đấu giá này đã KẾT THÚC.\n");
                bidAmountField.setDisable(true); // Khóa ô nhập giá
              } else if (!"Chưa có".equals(currentBidder)) {
                logArea.appendText(">> Người đang giữ giá cao nhất là " + currentBidder + "\n");
              } else {
                logArea.appendText(">> Chưa có ai đặt giá. Hãy là người đầu tiên!\n");
              }
              break;

            case "NEW_BID_BROADCAST":
              JsonNode payloadNode = objectMapper.readTree(message.getPayload());
              String targetAuction = payloadNode.get("auctionId").asText();

              // Chỉ cập nhật UI nếu gói tin thuộc về đúng phòng đấu giá này
              if (this.auctionId.equals(targetAuction)) {
                double newPrice = payloadNode.get("newPrice").asDouble();
                // Chú ý: Lấy đúng key "highestBidder" từ Server gửi về
                String bidder = payloadNode.get("highestBidder").asText();

                currentPriceLabel.setText(String.format("Giá cao nhất: $%.2f", newPrice));
                logArea.appendText(">> " + bidder + " vừa nâng giá lên $" + newPrice + "\n");
              }
              break;

            case "BID_SUCCESS":
              logArea.appendText("==> Chúc mừng! Bạn đang là người trả giá cao nhất.\n");
              bidAmountField.clear(); // Xóa trắng ô nhập để tiện đặt giá lần sau
              break;

            case "ERROR":
              // Hiển thị popup lỗi khi Server từ chối (ví dụ: Giá đặt quá thấp)
              showAlert("Thất bại", message.getPayload());
              break;

            case "SERVER_DISCONNECTED":
              showAlert("Mất kết nối", "Máy chủ đã đóng. Phiên đấu giá kết thúc.");
              handleLeaveRoom();
              break;
          }
        } catch (Exception e) {
          System.err.println("Lỗi xử lý UI: " + e.getMessage());
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi parse JSON trong Room: " + jsonMessage);
    }
  }

  @FXML
  private void handlePlaceBid() {
    String amountText = bidAmountField.getText().trim();
    if (amountText.isEmpty()) return;

    try {
      double amount = Double.parseDouble(amountText);

      // Xây dựng JSON payload khớp 100% với MessageRouter của Server
      // Do Client thiết kế tối giản không có ID riêng, ta dùng tên (currentUser) cho cả userId và username
      String payload = String.format(
          "{\"auctionId\":\"%s\", \"userId\":\"%s\", \"username\":\"%s\", \"amount\":%s}",
          auctionId, currentUser, currentUser, amount
      );

      String requestJson = String.format("{\"action\":\"PLACE_BID\", \"payload\":%s}", escapeJson(payload));
      networkClient.sendMessage(requestJson);

    } catch (NumberFormatException e) {
      showAlert("Lỗi nhập liệu", "Vui lòng nhập một con số hợp lệ (ví dụ: 1500.50)");
    }
  }

  @FXML
  private void handleLeaveRoom() {
    // Trả lại quyền nhận tin nhắn cho Dashboard
    SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + currentUser);
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private String escapeJson(String raw) {
    return "\"" + raw.replace("\"", "\\\"") + "\"";
  }
}