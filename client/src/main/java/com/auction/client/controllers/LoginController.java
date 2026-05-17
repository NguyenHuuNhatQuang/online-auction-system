package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class LoginController {

  @FXML
  private TextField usernameField;

  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();

    if (username.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập tên của bạn để tiếp tục!");
      return;
    }

    // Lưu tên người dùng vào SceneManager để dùng sau này
    SceneManager.getInstance().setCurrentUser(username);

    // Chuyển sang màn hình Dashboard (Chúng ta sẽ tạo file này ở bước kế tiếp)
    System.out.println("[UI] Người dùng đăng nhập: " + username);
    SceneManager.getInstance().switchScene("/fxml/dashboard.fxml", "Sảnh Chờ - " + username);
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}