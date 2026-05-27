package com.auction.server;

import com.auction.server.database.DatabaseConnection;
import com.auction.server.database.DatabaseWriteQueue;
import com.auction.server.network.AuctionServer;
import com.auction.server.services.AuctionManager;
import com.auction.server.services.AuctionScheduler;
import com.auction.server.services.BiddingService;

public class Main {
  private static final int DEFAULT_PORT = 8080;

  public static void main(String[] args) {
    System.out.println("==================================================");
    System.out.println("      HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER       ");
    System.out.println("==================================================");

    // 1. Khởi tạo Database và nạp dữ liệu lõi
    DatabaseConnection.initDatabase();
    System.out.println("[System] Đang nạp dữ liệu hệ thống...");
    AuctionManager.getInstance();

    // 2. Chuẩn bị các dịch vụ phụ thuộc
    BiddingService biddingService = new BiddingService();
    AuctionServer server = new AuctionServer(DEFAULT_PORT);

    // 3. Cấu hình và Bật luồng đếm ngược (PHẢI INIT TRƯỚC KHI START)
    AuctionScheduler.getInstance().init(biddingService, server);
    AuctionScheduler.getInstance().start();

    // 4. Đăng ký quy trình dọn dẹp khi tắt Server
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n[System] Nhận tín hiệu tắt máy chủ. Đang tiến hành dọn dẹp...");
      server.stopServer();
      AuctionScheduler.getInstance().stop();
      DatabaseWriteQueue.getInstance().shutdown();
      System.out.println("[System] Đã tắt máy chủ an toàn.");
    }));

    // 5. Khởi động Server Socket
    server.startServer();
  }
}