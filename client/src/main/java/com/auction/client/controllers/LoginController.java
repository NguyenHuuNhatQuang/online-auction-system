package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField; // Khai báo thêm password

  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    // Vừa vào màn hình login, giành quyền lắng nghe kết quả đăng nhập từ Server
    SceneManager.getInstance().getNetworkClient().setOnMessageReceived(this::handleServerResponse);
  }

  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    if (username.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    // Đóng gói JSON gửi yêu cầu đăng nhập
    String payload = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
    String request = String.format("{\"action\":\"LOGIN\", \"payload\":%s}", escapeJson(payload));

    SceneManager.getInstance().getNetworkClient().sendMessage(request);
  }

  @FXML
  private void handleRegisterBidder() {
    sendRegisterRequest("BIDDER");
  }

  @FXML
  private void handleRegisterSeller() {
    sendRegisterRequest("SELLER");
  }

  private void sendRegisterRequest(String role) {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();
    if (username.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên và mật khẩu để đăng ký!");
      return;
    }
    String payload = String.format("{\"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}", username, password, role);
    String request = String.format("{\"action\":\"REGISTER\", \"payload\":%s}", escapeJson(payload));
    SceneManager.getInstance().getNetworkClient().sendMessage(request);
  }

  private void handleServerResponse(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);

      Platform.runLater(() -> {
        try {
          if ("LOGIN_SUCCESS".equals(message.getAction())) {
            // Bóc tách dữ liệu Server trả về
            JsonNode payloadNode = objectMapper.readTree(message.getPayload());
            String username = payloadNode.get("username").asText();
            String role = payloadNode.get("role").asText();

            // Lưu thông tin vào phiên làm việc (Session)
            SceneManager.getInstance().setCurrentUser(username);
            SceneManager.getInstance().setUserRole(role);

            SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + username + " (" + role + ")");
          } else if ("REGISTER_SUCCESS".equals(message.getAction())) {
            showAlert("Thành công", "Đăng ký tài khoản thành công! Vui lòng bấm Đăng nhập.");
          } else if ("ERROR".equals(message.getAction())) {
            showAlert("Đăng nhập thất bại", message.getPayload());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi parse JSON Login: " + e.getMessage());
    }
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