package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;

public class AdminDashboardController {

  @FXML private ListView<String> userListView;
  @FXML private ListView<String> allItemListView;
  @FXML private ListView<String> allAuctionListView;

  private final java.util.List<String> itemIds = new java.util.ArrayList<>();
  private final java.util.List<String> auctionIds = new java.util.ArrayList<>();
  private NetworkClient networkClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    networkClient.setOnMessageReceived(this::handleServerMessage);

    // Yêu cầu lấy danh sách người dùng ngay khi mở Tab
    networkClient.sendMessage("{\"action\":\"ADMIN_GET_USERS\", \"payload\":\"\"}");
    refreshItems();
    refreshAuctions();
  }

  private void handleServerMessage(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);
      Platform.runLater(() -> {
        try {
          if ("ADMIN_USER_LIST".equals(message.getAction())) {
            JsonNode arrayNode = objectMapper.readTree(message.getPayload());
            userListView.getItems().clear();

            for (JsonNode node : arrayNode) {
              String username = node.get("username").asText();
              String role = node.get("role").asText();
              userListView.getItems().add(String.format("[%s] %s", role, username));
            }
          } else if ("ADMIN_ALL_ITEMS_LIST".equals(message.getAction())) {
            JsonNode arrayNode = objectMapper.readTree(message.getPayload());
            allItemListView.getItems().clear();
            itemIds.clear();
            for (JsonNode node : arrayNode) {
              itemIds.add(node.get("id").asText());
              allItemListView.getItems().add(String.format("[%s] %s - %s", node.get("type").asText(), node.get("name").asText(), node.get("desc").asText()));
            }
          } else if ("ACTIVE_AUCTIONS_LIST".equals(message.getAction())) {
            // Admin tái sử dụng chung gói tin danh sách đấu giá của hệ thống
            JsonNode arrayNode = objectMapper.readTree(message.getPayload());
            allAuctionListView.getItems().clear();
            auctionIds.clear();
            for (JsonNode node : arrayNode) {
              auctionIds.add(node.get("auctionId").asText());
              allAuctionListView.getItems().add(String.format("[%s] %s | %s | $%.2f", node.get("status").asText(), node.get("auctionId").asText(), node.get("itemName").asText(), node.get("currentPrice").asDouble()));
            }
          } else if ("ERROR".equals(message.getAction())) {
              showAlert("Lỗi", message.getPayload());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi parse JSON ở Admin: " + e.getMessage());
    }
  }

  @FXML
  private void handleMakeAdmin() {
    changeSelectedUserRole("ADMIN");
  }

  @FXML
  private void handleMakeSeller() {
    changeSelectedUserRole("SELLER");
  }

  @FXML
  private void handleMakeBidder() {
    changeSelectedUserRole("BIDDER");
  }

  @FXML
  private void handleBanUser() {
    changeSelectedUserRole("BANNED");
  }

  @FXML
  private void refreshItems() {
    networkClient.sendMessage("{\"action\":\"ADMIN_GET_ALL_ITEMS\", \"payload\":\"\"}");
  }

  @FXML
  private void refreshAuctions() {
    networkClient.sendMessage("{\"action\":\"GET_ACTIVE_AUCTIONS\", \"payload\":\"\"}");
  }

  @FXML
  private void handleAdminDeleteItem() {
    int index = allItemListView.getSelectionModel().getSelectedIndex();
    if (index >= 0) {
      String payload = String.format("{\"itemId\":\"%s\"}", itemIds.get(index));
      networkClient.sendMessage(String.format("{\"action\":\"ADMIN_DELETE_ITEM\", \"payload\":%s}", escapeJson(payload)));
    } else {
      showAlert("Thông báo", "Vui lòng chọn một sản phẩm để xóa.");
    }
  }

  @FXML
  private void handleForceStopAuction() {
    int index = allAuctionListView.getSelectionModel().getSelectedIndex();
    if (index >= 0) {
      String status = allAuctionListView.getItems().get(index);
      if (status.contains("[RUNNING]")) {
        String payload = String.format("{\"auctionId\":\"%s\"}", auctionIds.get(index));
        networkClient.sendMessage(String.format("{\"action\":\"ADMIN_FORCE_STOP_AUCTION\", \"payload\":%s}", escapeJson(payload)));
      } else {
        showAlert("Thông báo", "Chỉ có thể dừng khẩn cấp các phiên đang chạy (RUNNING)!");
      }
    } else {
      showAlert("Thông báo", "Vui lòng chọn một phiên đấu giá.");
    }
  }

  private void changeSelectedUserRole(String newRole) {
    String selected = userListView.getSelectionModel().getSelectedItem();
    if (selected == null) {
      showAlert("Thông báo", "Vui lòng chọn một người dùng trong danh sách!");
      return;
    }

    String username = selected.substring(selected.indexOf("]") + 1).trim();
    String currentUser = SceneManager.getInstance().getCurrentUser();

    // Bảo vệ tối đa: Không thể sửa quyền của chính mình đang đăng nhập
    if (username.equals(currentUser)) {
      showAlert("Cảnh báo", "Bạn không thể tự thay đổi quyền của chính mình!");
      return;
    }
    // Bảo vệ thêm: Tuyệt đối không ai được động vào tài khoản admin gốc
    if ("admin".equals(username)) {
      showAlert("Cảnh báo", "Bạn không thể thay đổi quyền của Admin tối cao!");
      return;
    }

    String payload = String.format("{\"username\":\"%s\", \"newRole\":\"%s\"}", username, newRole);
    networkClient.sendMessage(String.format("{\"action\":\"ADMIN_CHANGE_ROLE\", \"payload\":%s}", escapeJson(payload)));
  }

  @FXML
  private void handleBack() {
    String currentUser = SceneManager.getInstance().getCurrentUser();
    String role = SceneManager.getInstance().getUserRole();
    SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + currentUser + " (" + role + ")");
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private String escapeJson(String raw) {
    return "\"" + raw.replace("\"", "\\\"") + "\"";
  }
}