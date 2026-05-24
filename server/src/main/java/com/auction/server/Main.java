package com.auction.server;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.network.AuctionServer;

/**
 * Điểm neo khởi động (Entry Point) của toàn bộ hệ thống máy chủ.
 */
public class Main {

  /** Cổng mạng mặc định mà máy chủ sẽ sử dụng. */
  private static final int DEFAULT_PORT = 8080;

  public static void main(String[] args) {
    System.out.println("==================================================");
    System.out.println("      HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER       ");
    System.out.println("==================================================");

    // Khởi tạo Database ngay khi bật Server
    DatabaseConnection.initDatabase();

    // 1. Khởi tạo phiên bản máy chủ
    AuctionServer server = new AuctionServer(DEFAULT_PORT);

    // 2. Đăng ký Shutdown Hook (Móc nối sự kiện tắt ứng dụng)
    // Khi bạn nhấn Ctrl+C, hoặc đóng terminal, Java sẽ tự động chạy luồng này trước khi tắt hẳn.
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n[System] Nhận tín hiệu tắt máy chủ (SIGINT). Đang tiến hành dọn dẹp...");
      server.stopServer();

      // Đóng hàng đợi DB
      com.auction.server.database.DatabaseWriteQueue.getInstance().shutdown();
    }));

    // 3. Kích hoạt máy chủ
    // Lệnh này chứa vòng lặp while (isRunning) nên nó sẽ chạy vô hạn và giữ ứng dụng không bị tắt.
    server.startServer();
  }
}