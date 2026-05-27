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
import javafx.scene.control.Button;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AuctionRoomController {

  @FXML private Label roomTitleLabel;
  @FXML private Label currentPriceLabel;
  @FXML private TextField bidAmountField;
  @FXML private TextArea logArea;
  @FXML private Label itemNameLabel;
  @FXML private Label itemDescLabel;
  @FXML private Label timerLabel;
  @FXML private Label highestBidderLabel;
  @FXML private Button payButton;

  private NetworkClient networkClient;
  private String currentUser;
  private String auctionId;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Timeline countdownTimeline;
  private LocalDateTime endTime;

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

                  if (currentUser.equals(winner)) {
                    payButton.setVisible(true);
                    payButton.setManaged(true);
                  }
                }
                logArea.appendText("==============================\n");

                // Khóa UI không cho đặt giá nữa
                bidAmountField.setDisable(true);
              }
              break;

            case "AUCTION_PAID": { // Lắng nghe phản hồi thanh toán thành công
              JsonNode paidNode = objectMapper.readTree(message.getPayload());
              if (this.auctionId.equals(paidNode.get("auctionId").asText())) {
                showAlert("Thành công", "Bạn đã thanh toán thành công cho phiên đấu giá này!");
                handleLeaveRoom(); // Trục xuất ra Sảnh chờ sau khi thanh toán xong
              }
              break;
            }

            case "AUCTION_STATE": {
              JsonNode stateNode = objectMapper.readTree(message.getPayload());
              itemNameLabel.setText("Sản phẩm: " + stateNode.get("itemName").asText());
              itemDescLabel.setText("Mô tả: " + stateNode.get("itemDesc").asText());
              currentPriceLabel.setText(String.format("Giá hiện tại: $%.2f", stateNode.get("currentPrice").asDouble()));
              highestBidderLabel.setText("Người giữ giá: " + stateNode.get("highestBidder").asText());

              // Xóa trắng log cũ để chuẩn bị in lịch sử mới
              logArea.clear();
              logArea.appendText("=== CHÀO MỪNG ĐẾN PHÒNG ĐẤU GIÁ ===\n");

              // Đọc mảng lịch sử (nếu có) và in ra màn hình
              if (stateNode.has("bidHistory")) {
                JsonNode historyArray = stateNode.get("bidHistory");
                for (JsonNode txNode : historyArray) {
                  String bidder = txNode.get("bidder").asText();
                  double amount = txNode.get("amount").asDouble();
                  logArea.appendText(">> Lịch sử: " + bidder + " đã đặt giá $" + amount + "\n");
                }
              }

              String status = stateNode.get("status").asText();
              if ("CLOSED".equals(status) || "FINISHED".equals(status) || "PAID".equals(status)) {
                timerLabel.setText("ĐÃ KẾT THÚC");
                bidAmountField.setDisable(true);
              } else {
                this.endTime = LocalDateTime.parse(stateNode.get("endTime").asText());
                startCountdownTimer();
              }
              break;
            }

            case "NEW_BID_BROADCAST": {
              JsonNode payloadNode = objectMapper.readTree(message.getPayload());
              if (this.auctionId.equals(payloadNode.get("auctionId").asText())) {
                double newPrice = payloadNode.get("newPrice").asDouble();
                String bidder = payloadNode.get("highestBidder").asText();

                currentPriceLabel.setText(String.format("Giá cao nhất: $%.2f", newPrice));
                highestBidderLabel.setText("Người giữ giá: " + bidder); // Cập nhật người giữ giá
                logArea.appendText(">> " + bidder + " vừa nâng giá lên $" + newPrice + "\n");
              }
              break;
            }

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
      if (amount <= 0) {
        showAlert("Lỗi", "Giá đặt phải lớn hơn 0!");
        return;
      }

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
  private void handlePay() {
    // Gửi lệnh mạng lên Server
    String request = String.format("{\"action\":\"PAY_AUCTION\", \"payload\":\"{\\\"auctionId\\\":\\\"%s\\\"}\"}", auctionId);
    networkClient.sendMessage(request);

    // Tạm thời vô hiệu hóa nút tránh bấm 2 lần
    payButton.setDisable(true);
    payButton.setText("Đang xử lý...");
  }

  private void startCountdownTimer() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      long secondsRemaining = LocalDateTime.now().until(endTime, ChronoUnit.SECONDS);
      if (secondsRemaining <= 0) {
        timerLabel.setText("ĐÃ KẾT THÚC");
        countdownTimeline.stop();
      } else {
        long minutes = secondsRemaining / 60;
        long seconds = secondsRemaining % 60;
        timerLabel.setText(String.format("Thời gian còn lại: %02d:%02d", minutes, seconds));
      }
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  @FXML
  private void handleLeaveRoom() {
    if (countdownTimeline != null) countdownTimeline.stop();
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