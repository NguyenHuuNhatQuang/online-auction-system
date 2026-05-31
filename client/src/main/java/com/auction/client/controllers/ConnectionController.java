package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ConnectionController {

  @FXML private TextField ipField;
  @FXML private TextField portField;
  @FXML private Label statusLabel;

  @FXML
  private void handleConnect() {
    String ip = ipField.getText().trim();
    int port;

    // THÊM ĐOẠN KIỂM TRA BẢO VỆ DỮ LIỆU NHẬP VÀO
    try {
      port = Integer.parseInt(portField.getText().trim());
      if (port <= 0 || port > 65535) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      statusLabel.setStyle("-fx-text-fill: red;");
      statusLabel.setText("Lỗi: Port phải là một số từ 1 đến 65535!");
      return;
    }

    statusLabel.setStyle("-fx-text-fill: blue;");
    statusLabel.setText("Đang kết nối đến " + ip + ":" + port + "...");

    // Chạy việc kết nối trên luồng riêng để không làm đơ giao diện
    new Thread(() -> {
      try {
        NetworkClient networkClient = new NetworkClient(ip, port);
        networkClient.connect();

        // Lưu instance mạng vào biến toàn cục của SceneManager
        SceneManager.getInstance().setNetworkClient(networkClient);

        // Chuyển sang màn hình Đăng nhập nếu kết nối thành công
        Platform.runLater(() -> {
          SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá");
        });
      } catch (Exception e) {
        Platform.runLater(() -> {
          statusLabel.setStyle("-fx-text-fill: red;");
          statusLabel.setText("Lỗi: Không thể kết nối đến máy chủ!");
        });
      }
    }).start();
  }
}