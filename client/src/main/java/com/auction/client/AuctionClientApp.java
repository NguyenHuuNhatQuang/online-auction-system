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
    // 1. Khởi tạo mạng ngầm (Không in ra Console nữa, để Controller tự xử lý sau)
    networkClient = new NetworkClient();
    try {
      networkClient.connect("127.0.0.1", 8080, message -> {
        // Tạm thời chưa xử lý gì ở đây, để dành cho các Controller sau
        System.out.println("[Client nhận]: " + message);
      });
    } catch (IOException e) {
      System.err.println("Không thể kết nối đến máy chủ. Vui lòng bật Server trước!");
      // Vẫn cho hiện giao diện Login dù chưa có mạng để test UI
    }

    // 2. Khởi tạo SceneManager và chuyển đến màn hình Login
    SceneManager.getInstance().init(primaryStage, networkClient);
    SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá");
  }

  @Override
  public void stop() {
    if (networkClient != null) {
      networkClient.disconnect();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}