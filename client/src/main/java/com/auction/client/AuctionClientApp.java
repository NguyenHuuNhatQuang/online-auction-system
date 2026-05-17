package com.auction.client;

import com.auction.client.network.NetworkClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Lớp khởi động chính của ứng dụng Client sử dụng JavaFX.
 */
public class AuctionClientApp extends Application {

  private NetworkClient networkClient;

  @Override
  public void start(Stage primaryStage) {
    // 1. Tạo một giao diện tạm thời rất đơn giản
    Label statusLabel = new Label("Đang kết nối đến Server...");
    StackPane root = new StackPane(statusLabel);
    Scene scene = new Scene(root, 400, 300);

    primaryStage.setTitle("Hệ thống Đấu giá (Test Kết Nối)");
    primaryStage.setScene(scene);
    primaryStage.show();

    // 2. Khởi tạo và kết nối Mạng
    networkClient = new NetworkClient();

    // Chạy kết nối mạng trong một luồng riêng để không làm đơ giao diện
    new Thread(() -> {
      try {
        // Kết nối tới Server ở localhost:8080
        // Hàm Callback (msg -> ...) sẽ in tin nhắn từ Server ra màn hình Console
        networkClient.connect("127.0.0.1", 8080, msg -> {
          System.out.println("[Phản hồi từ Server]: " + msg);
        });

        // Nếu kết nối thành công, cập nhật chữ trên màn hình (Phải dùng Platform.runLater)
        Platform.runLater(() -> statusLabel.setText("Đã kết nối thành công tới Server!"));

        // Gửi thử một chuỗi linh tinh lên Server xem nó phản ứng thế nào
        networkClient.sendMessage("{\"action\":\"TEST\", \"payload\":\"Hello Server\"}");

      } catch (IOException e) {
        Platform.runLater(() -> statusLabel.setText("Lỗi kết nối: Không tìm thấy Server."));
      }
    }).start();
  }

  @Override
  public void stop() {
    // Hàm này tự động chạy khi người dùng bấm dấu X tắt cửa sổ
    if (networkClient != null) {
      networkClient.disconnect();
    }
  }

  public static void main(String[] args) {
    launch(args); // Lệnh mồi của JavaFX
  }
}