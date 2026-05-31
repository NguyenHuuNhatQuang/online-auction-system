package com.auction.client;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class AuctionClientApp extends Application {

  private NetworkClient networkClient;

  @Override
  public void start(Stage primaryStage) {
    // Xác định địa chỉ máy chủ. Thứ tự ưu tiên:
    //   1) Tham số dòng lệnh:  java -jar client.jar <host> <port>
    //   2) Biến môi trường:    AUCTION_HOST / AUCTION_PORT
    //   3) Mặc định:           127.0.0.1:8080 (chạy nội bộ)
    // Nhờ vậy có thể trỏ client tới địa chỉ public (vd ngrok: 0.tcp.ngrok.io 17234).
    String host = "127.0.0.1";
    int port = 8080;

    java.util.List<String> params = (getParameters() != null)
        ? getParameters().getRaw() : java.util.List.of();
    String envHost = System.getenv("AUCTION_HOST");
    String envPort = System.getenv("AUCTION_PORT");

    if (!params.isEmpty() && !params.get(0).isBlank()) {
      host = params.get(0).trim();
    } else if (envHost != null && !envHost.isBlank()) {
      host = envHost.trim();
    }

    String portText = (params.size() >= 2) ? params.get(1) : envPort;
    if (portText != null && !portText.isBlank()) {
      try {
        port = Integer.parseInt(portText.trim());
      } catch (NumberFormatException ex) {
        System.err.println("[Client] Cổng không hợp lệ '" + portText + "', dùng mặc định 8080.");
      }
    }

    // 1. Khởi tạo mạng ngầm (Không in ra Console nữa, để Controller tự xử lý sau)
    System.out.println("[Client] Đang kết nối tới máy chủ " + host + ":" + port + " ...");
    networkClient = new NetworkClient();
    try {
      networkClient.connect(host, port, message -> {
        // Tạm thời chưa xử lý gì ở đây, để dành cho các Controller sau
        System.out.println("[Client nhận]: " + message);
      });
    } catch (IOException e) {
      System.err.println("Không thể kết nối đến máy chủ " + host + ":" + port
          + ". Vui lòng bật Server trước!");
      // Vẫn cho hiện giao diện Login dù chưa có mạng để test UI
    }

    // 2. Khởi tạo SceneManager và chuyển đến màn hình Login
    SceneManager.getInstance().init(primaryStage, networkClient);
    SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá");
  }

  @Override
  public void stop() {
    System.out.println("[Client] Đang tắt ứng dụng...");
    if (networkClient != null) {
      networkClient.disconnect();
    }

    // Ép máy ảo Java tắt hoàn toàn, tiêu diệt mọi luồng chạy ngầm đang bị kẹt
    System.exit(0);
  }

  public static void main(String[] args) {
    launch(args);
  }
}