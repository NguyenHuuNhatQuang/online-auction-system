package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Màn hình "Kết nối máy chủ": cho người dùng nhập IP/Host và Port của Server
 * trước khi vào màn hình đăng nhập. Nhờ vậy các máy ở mạng khác nhau (LAN, ngrok…)
 * có thể trỏ tới đúng server mà không cần truyền tham số dòng lệnh.
 */
public class ConnectionController {

  @FXML private TextField ipField;
  @FXML private TextField portField;
  @FXML private Label statusLabel;

  /** Điền sẵn IP/Port gợi ý (từ tham số dòng lệnh / biến môi trường / mặc định). */
  @FXML
  private void initialize() {
    String host = SceneManager.getInstance().getDefaultHost();
    int port = SceneManager.getInstance().getDefaultPort();
    if (ipField != null && host != null && !host.isBlank()) {
      ipField.setText(host);
    }
    if (portField != null) {
      portField.setText(String.valueOf(port));
    }
  }

  @FXML
  private void handleConnect() {
    String ip = ipField.getText().trim();
    int port;

    // Kiểm tra dữ liệu nhập
    try {
      port = Integer.parseInt(portField.getText().trim());
      if (port <= 0 || port > 65535) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      statusLabel.setStyle("-fx-text-fill: #ef4444;");
      statusLabel.setText("Lỗi: Port phải là một số từ 1 đến 65535!");
      return;
    }
    if (ip.isBlank()) {
      statusLabel.setStyle("-fx-text-fill: #ef4444;");
      statusLabel.setText("Lỗi: Vui lòng nhập địa chỉ IP/Host của máy chủ!");
      return;
    }

    statusLabel.setStyle("-fx-text-fill: #2563eb;");
    statusLabel.setText("Đang kết nối đến " + ip + ":" + port + "...");

    final String hostFinal = ip;
    final int portFinal = port;

    // Kết nối trên luồng riêng để không làm đơ giao diện
    new Thread(() -> {
      try {
        NetworkClient networkClient = new NetworkClient();
        networkClient.connect(hostFinal, portFinal,
            message -> System.out.println("[Client nhận]: " + message));

        // Lưu instance mạng vào SceneManager để các màn hình sau dùng chung
        SceneManager.getInstance().setNetworkClient(networkClient);

        Platform.runLater(() ->
            SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá"));
      } catch (Exception ex) {
        Platform.runLater(() -> {
          statusLabel.setStyle("-fx-text-fill: #ef4444;");
          statusLabel.setText("Không thể kết nối tới " + hostFinal + ":" + portFinal
              + ". Kiểm tra IP/Port và chắc chắn Server đã bật.");
        });
      }
    }).start();
  }
}
