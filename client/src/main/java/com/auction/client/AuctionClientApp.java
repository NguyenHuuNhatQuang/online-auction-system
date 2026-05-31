package com.auction.client;

import com.auction.client.core.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class AuctionClientApp extends Application {

  @Override
  public void start(Stage primaryStage) {
    // 1. Khởi tạo SceneManager (Lúc này truyền null cho NetworkClient vì chưa kết nối)
    SceneManager.getInstance().init(primaryStage, null);

    // 2. Chuyển hướng người dùng đến màn hình Kết nối (Connection Screen) thay vì Login
    SceneManager.getInstance().switchScene("/fxml/connection.fxml", "Thiết lập Kết nối với Máy chủ");
  }

  @Override
  public void stop() {
    System.out.println("[Client] Đang tắt ứng dụng...");

    // Lấy NetworkClient từ SceneManager để đóng kết nối an toàn trước khi tắt App
    if (SceneManager.getInstance().getNetworkClient() != null) {
      SceneManager.getInstance().getNetworkClient().disconnect();
    }

    // Ép máy ảo Java tắt hoàn toàn, tiêu diệt mọi luồng chạy ngầm đang bị kẹt
    System.exit(0);
  }

  public static void main(String[] args) {
    launch(args);
  }
}