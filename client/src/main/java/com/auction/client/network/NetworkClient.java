package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Quản lý kết nối Socket từ phía Client.
 * Chạy một luồng ngầm để liên tục lắng nghe dữ liệu từ Server và đẩy lên UI.
 */
public class NetworkClient {

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private Thread listeningThread;

  // Một "phễu" (Callback) để đẩy tin nhắn nhận được lên cho JavaFX xử lý
  private Consumer<String> onMessageReceived;

  /**
   * Khởi tạo kết nối mạng tới máy chủ.
   *
   * @param host IP của máy chủ (VD: "127.0.0.1").
   * @param port Cổng mạng (VD: 8080).
   * @param onMessageReceived Hàm Callback để xử lý khi có chuỗi JSON từ Server gửi về.
   */
  public void connect(String host, int port, Consumer<String> onMessageReceived) throws IOException {
    this.socket = new Socket(host, port);
    this.out = new PrintWriter(socket.getOutputStream(), true);
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.onMessageReceived = onMessageReceived;

    System.out.println("[NetworkClient] Đã kết nối tới Server: " + host + ":" + port);
    startListening();
  }

  /**
   * Bật một luồng chạy ngầm để vểnh tai nghe thông điệp từ Server.
   */
  private void startListening() {
    listeningThread = new Thread(() -> {
      try {
        String line;
        // Vòng lặp chặn (Blocking), nằm im đợi cho đến khi Server gửi tin nhắn
        while ((line = in.readLine()) != null) {
          System.out.println("[NetworkClient] Nhận từ Server: " + line);
          if (onMessageReceived != null) {
            onMessageReceived.accept(line);
          }
          // Nếu tin nhắn là ngắt từ Server, tự động phá vòng lặp
          if (line.contains("SERVER_DISCONNECTED")) {
            break;
          }
        }
      } catch (IOException e) {
        // Chạy vào đây nếu đường truyền bị bẻ gãy đột ngột
        System.out.println("[NetworkClient] Đường truyền mạng bị ngắt do lỗi: " + e.getMessage());
      } finally {
        // KHỐI FINALLY: Luôn luôn được gọi dù thoát vòng lặp bình thường hay bị văng lỗi
        System.out.println("[NetworkClient] Kết nối tới Server đã khép lại.");
        if (onMessageReceived != null) {
          onMessageReceived.accept("{\"action\":\"SERVER_DISCONNECTED\"}");
        }

        this.disconnect();
      }
    });

    listeningThread.setDaemon(true);
    listeningThread.start();
  }

  /**
   * Gửi một chuỗi (thường là JSON) lên Server.
   */
  public void sendMessage(String message) {
    if (out != null && !socket.isClosed()) {
      out.println(message);
    }
  }

  /**
   * Chủ động ngắt kết nối an toàn từ phía Client.
   */
  public void disconnect() {
    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null && !socket.isClosed()) socket.close();
      System.out.println("[NetworkClient] Đã chủ động đóng kết nối.");
    } catch (IOException e) {
      System.err.println("[NetworkClient] Lỗi khi đóng kết nối: " + e.getMessage());
    }
  }

  /**
   * Cập nhật lại hàm xử lý tin nhắn.
   * Giúp mỗi màn hình (Login, Dashboard, AuctionRoom) có thể tự giành quyền
   * xử lý tin nhắn từ Server theo cách riêng của nó.
   */
  public void setOnMessageReceived(Consumer<String> onMessageReceived) {
    this.onMessageReceived = onMessageReceived;
  }
}