package com.auction.client;

import com.auction.client.core.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class AuctionClientApp extends Application {

  @Override
  public void start(Stage primaryStage) {
    // Xác định địa chỉ máy chủ GỢI Ý cho màn hình "Kết nối máy chủ". Thứ tự ưu tiên:
    //   1) Tham số dòng lệnh:  java -jar client.jar <host> <port>
    //   2) Biến môi trường:    AUCTION_HOST / AUCTION_PORT
    //   3) Mặc định:           127.0.0.1:8080 (chạy nội bộ)
    // Người dùng vẫn có thể tự nhập/sửa IP & Port ngay trên màn hình kết nối
    // (vd trỏ tới LAN 192.168.x.x hoặc ngrok 0.tcp.ngrok.io).
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

    // Khởi tạo SceneManager rồi mở MÀN HÌNH KẾT NỐI trước (nhập IP/Port thủ công).
    // Việc connect thực sự do ConnectionController đảm nhận; gợi ý sẵn host/port ở trên.
    SceneManager.getInstance().init(primaryStage, null);
    SceneManager.getInstance().setDefaultHost(host);
    SceneManager.getInstance().setDefaultPort(port);
    SceneManager.getInstance().switchScene("/fxml/connection.fxml", "Kết nối máy chủ - BidNow");
  }

  @Override
  public void stop() {
    System.out.println("[Client] Đang tắt ứng dụng...");
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