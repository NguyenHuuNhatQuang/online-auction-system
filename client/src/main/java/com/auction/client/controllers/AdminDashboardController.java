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

  private NetworkClient networkClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    networkClient.setOnMessageReceived(this::handleServerMessage);

    // Yêu cầu lấy danh sách người dùng ngay khi mở Tab
    networkClient.sendMessage("{\"action\":\"ADMIN_GET_USERS\", \"payload\":\"\"}");
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