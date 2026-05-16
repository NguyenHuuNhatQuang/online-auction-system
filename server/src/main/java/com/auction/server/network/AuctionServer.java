package com.auction.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Máy chủ quản lý các kết nối Socket.
 * Đóng vai trò là Subject trong Observer Pattern để phát sóng thông điệp Realtime.
 */
public class AuctionServer {

  private final int port;
  private boolean isRunning;

  // Thread Pool tối ưu việc xử lý nhiều luồng kết nối cùng lúc
  private final ExecutorService threadPool;

  // Lưu trữ danh sách các Client đang trực tuyến một cách an toàn (Thread-safe)
  private final Set<ClientHandler> activeClients;

  public AuctionServer(int port) {
    this.port = port;
    this.isRunning = false;
    // Giới hạn phục vụ tối đa 100 người dùng cùng lúc
    this.threadPool = Executors.newFixedThreadPool(100);
    this.activeClients = ConcurrentHashMap.newKeySet();
  }

  /**
   * Khởi động máy chủ, lắng nghe các kết nối đến.
   */
  public void startServer() {
    isRunning = true;
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("[AuctionServer] Máy chủ đang chạy tại port " + port + "...");

      while (isRunning) {
        // Lệnh chặn (Blocking): Chờ cho đến khi có một Client gọi tới
        Socket clientSocket = serverSocket.accept();

        // Khởi tạo handler cho Client mới
        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
        activeClients.add(clientHandler);

        // Ném handler vào ThreadPool để nó chạy nền, máy chủ tiếp tục quay lại trực cổng đón người mới
        threadPool.execute(clientHandler);
      }
    } catch (IOException e) {
      System.err.println("[AuctionServer] Lỗi khởi động máy chủ: " + e.getMessage());
    } finally {
      stopServer();
    }
  }

  /**
   * Gỡ bỏ một Client khỏi danh sách trực tuyến khi họ thoát.
   */
  public void removeClient(ClientHandler clientHandler) {
    activeClients.remove(clientHandler);
  }

  /**
   * REALTIME UPDATE: Gửi thông điệp (ví dụ: thông báo có người vừa đặt giá) tới TẤT CẢ các Client.
   * Đây chính là cơ chế để thực hiện chức năng Push/Observer Pattern từ máy chủ.
   *
   * @param message Chuỗi dữ liệu (JSON) cần phát sóng.
   */
  public void broadcastMessage(String message) {
    for (ClientHandler client : activeClients) {
      client.sendMessage(message);
    }
  }

  /**
   * Đóng máy chủ và giải phóng bộ nhớ.
   */
  public void stopServer() {
    isRunning = false;
    threadPool.shutdown();
    System.out.println("[AuctionServer] Đã tắt máy chủ.");
  }
}