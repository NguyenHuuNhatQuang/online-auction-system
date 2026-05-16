package com.auction.server.network;

import com.auction.server.services.AuctionScheduler;
import com.auction.server.services.BiddingService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Máy chủ trung tâm chịu trách nhiệm quản lý kết nối mạng qua giao thức Socket.
 * Đóng vai trò cấu nối nhận yêu cầu từ các Client và phát sóng các sự kiện
 * cập nhật giá theo thời gian thực (Realtime Update) áp dụng mẫu thiết kế Observer.
 */
public class AuctionServer {

  /** Cổng mạng (Port) mà máy chủ sẽ lắng nghe kết nối. */
  private final int port;

  /** Trạng thái hoạt động của máy chủ (true: đang chạy, false: đã dừng). */
  private boolean isRunning;

  /** Quản lý tập hợp các luồng (Thread Pool) phục vụ luồng xử lý I/O của Client. */
  private final ExecutorService threadPool;

  /** Danh sách lưu trữ an toàn đa luồng chứa các kết nối Client đang trực tuyến. */
  private final Set<ClientHandler> activeClients;

  /** Bộ định thời chạy ngầm tự động quét và đóng các phiên đấu giá hết hạn. */
  private AuctionScheduler scheduler;

  /**
   * Khởi tạo một máy chủ đấu giá mới với cổng mạng xác định.
   * Mặc định cấp phát Thread Pool tối đa 100 kết nối đồng thời.
   *
   * @param port Cổng mạng mạng máy chủ sẽ sử dụng để lắng nghe kết nối.
   */
  public AuctionServer(int port) {
    this.port = port;
    this.isRunning = false;
    this.threadPool = Executors.newFixedThreadPool(100);
    this.activeClients = ConcurrentHashMap.newKeySet();
  }

  /**
   * Khởi động máy chủ, kích hoạt bộ quét thời gian ngầm và bắt đầu vòng lặp
   * chấp nhận (accept) các kết nối Socket đi vào từ phía Client.
   */
  public void startServer() {
    this.isRunning = true;

    // Khởi tạo và kích hoạt bộ quét thời gian đấu giá tự động
    BiddingService biddingService = new BiddingService();
    this.scheduler = new AuctionScheduler(biddingService, this);
    this.scheduler.start();

    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("[AuctionServer] Máy chủ đang hoạt động tại port " + port + "...");

      while (isRunning) {
        // Lệnh chặn (Blocking): Chờ cho đến khi có một kết nối từ Client gửi tới
        Socket clientSocket = serverSocket.accept();

        // Khởi tạo đối tượng xử lý riêng biệt cho Client mới này
        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
        activeClients.add(clientHandler);

        // Giao việc giao tiếp I/O cho một luồng độc lập trong Thread Pool quản lý
        threadPool.execute(clientHandler);
      }
    } catch (IOException e) {
      System.err.println("[AuctionServer] Lỗi phát sinh trên Server Socket: " + e.getMessage());
    } finally {
      stopServer();
    }
  }

  /**
   * Loại bỏ một Client Handler khỏi danh sách quản lý trực tuyến.
   * Thường được gọi khi Client ngắt kết nối hoặc xảy ra sự cố đường truyền.
   *
   * @param clientHandler Đối tượng xử lý kết nối cần gỡ bỏ.
   */
  public void removeClient(ClientHandler clientHandler) {
    if (clientHandler != null) {
      activeClients.remove(clientHandler);
    }
  }

  /**
   * Cơ chế cập nhật thời gian thực (Realtime Broadcast).
   * Gửi một thông điệp chuỗi (thường là định dạng JSON) tới TẤT CẢ các Client
   * đang kết nối trực tuyến với hệ thống mà không cần cơ chế Polling lặp lại.
   *
   * @param message Chuỗi thông điệp cần phát sóng trên toàn hệ thống công cộng.
   */
  public void broadcastMessage(String message) {
    for (ClientHandler client : activeClients) {
      client.sendMessage(message);
    }
  }

  /**
   * Ngắt hoạt động của máy chủ một cách an toàn (Graceful Shutdown).
   * Tiến hành chủ động ngắt kết nối tất cả Client, thu hồi tài nguyên luồng
   * và tắt bộ quét thời gian.
   */
  public void stopServer() {
    this.isRunning = false;

    // 1. Hủy cấp phát tài nguyên luồng chạy ngầm
    if (scheduler != null) {
      scheduler.stop();
    }

    // 2. Chủ động ngắt kết nối toàn bộ Client đang trực tuyến
    if (!activeClients.isEmpty()) {
      System.out.println("[AuctionServer] Đang ngắt kết nối " + activeClients.size() + " Client hiện tại...");
      for (ClientHandler client : activeClients) {
        client.disconnect();
      }
      activeClients.clear(); // Làm sạch bộ nhớ danh sách
    }

    // 3. Tắt Thread Pool xử lý kết nối mạng
    if (threadPool != null && !threadPool.isShutdown()) {
      threadPool.shutdown();
    }

    System.out.println("[AuctionServer] Máy chủ đã dừng hoạt động an toàn.");
  }
}