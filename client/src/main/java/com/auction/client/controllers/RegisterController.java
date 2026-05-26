package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Màn hình Đăng ký riêng cho từng vai trò (BIDDER / SELLER).
 * Vai trò được truyền qua SceneManager.registerRole trước khi chuyển cảnh.
 * Gửi action REGISTER, lắng nghe REGISTER_SUCCESS / ERROR.
 */
public class RegisterController {

  @FXML private VBox heroPanel;
  @FXML private Label heroTitle;
  @FXML private Label heroLead;
  @FXML private Label heroSub;
  @FXML private Label formTitle;
  @FXML private Label roleBadge;
  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private Button submitButton;

  private String role;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @FXML
  public void initialize() {
    this.role = SceneManager.getInstance().getRegisterRole();
    if (role == null) role = "BIDDER";

    boolean seller = "SELLER".equalsIgnoreCase(role);

    // Tô màu hero + badge theo vai trò, đúng tinh thần mockup S2/S3
    heroPanel.getStyleClass().removeAll("hero-panel-bidder", "hero-panel-seller");
    heroPanel.getStyleClass().add(seller ? "hero-panel-seller" : "hero-panel-bidder");

    roleBadge.setText(role);
    roleBadge.getStyleClass().removeAll("badge-bidder", "badge-seller");
    roleBadge.getStyleClass().add(seller ? "badge-seller" : "badge-bidder");

    submitButton.getStyleClass().removeAll("btn-bidder", "btn-seller");
    submitButton.getStyleClass().add(seller ? "btn-seller" : "btn-bidder");
    submitButton.setText(seller ? "Xác nhận đăng ký Seller" : "Xác nhận đăng ký Bidder");

    if (seller) {
      heroTitle.setText("Seller");
      heroLead.setText("Bán hàng — mở phiên đấu giá riêng.");
      heroSub.setText("Tạo sản phẩm (Electronics / Art), tự ấn định giá khởi điểm và thời lượng, hệ thống tự kết thúc đúng giờ.");
    } else {
      heroTitle.setText("Bidder");
      heroLead.setText("Săn món đồ độc, đặt giá tự do.");
      heroSub.setText("Tham gia mọi phiên đang chạy, đặt giá realtime và thanh toán khi thắng.");
    }
    formTitle.setText("Tạo tài khoản");

    SceneManager.getInstance().getNetworkClient().setOnMessageReceived(this::handleServerResponse);
  }

  @FXML
  private void handleRegister() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();
    if (username.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }
    String payload = String.format("{\"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}", username, password, role);
    String request = String.format("{\"action\":\"REGISTER\", \"payload\":%s}", escapeJson(payload));
    SceneManager.getInstance().getNetworkClient().sendMessage(request);
  }

  @FXML
  private void handleBackToLogin() {
    SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá");
  }

  private void handleServerResponse(String jsonMessage) {
    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);
      Platform.runLater(() -> {
        if ("REGISTER_SUCCESS".equals(message.getAction())) {
          Alert ok = new Alert(Alert.AlertType.INFORMATION);
          ok.setTitle("Thành công");
          ok.setHeaderText(null);
          ok.setContentText("Tài khoản " + role + " đã được tạo. Vui lòng đăng nhập.");
          ok.showAndWait();
          handleBackToLogin();
        } else if ("ERROR".equals(message.getAction())) {
          showAlert("Đăng ký thất bại", message.getPayload());
        }
      });
    } catch (Exception e) {
      System.err.println("Lỗi parse JSON Register: " + e.getMessage());
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
