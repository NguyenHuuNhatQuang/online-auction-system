package com.auction.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Xử lý giao tiếp I/O với một Client cụ thể trên một luồng riêng biệt.
 * Lắng nghe thông điệp từ Client và đẩy thông điệp từ Server về Client.
 */
public class ClientHandler implements Runnable {

  private final Socket clientSocket;
  private final AuctionServer server;
  private PrintWriter out;
  private BufferedReader in;
  private String clientId;
  private boolean isClosed = false;

  public ClientHandler(Socket clientSocket, AuctionServer server) {
    this.clientSocket = clientSocket;
    this.server = server;
    this.clientId = clientSocket.getRemoteSocketAddress().toString();
  }

  @Override
  public void run() {
    // Khởi tạo MessageRouter cho client này (Nó dùng chung AuctionServer)
    MessageRouter router = new MessageRouter(server);

    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      System.out.println("[ClientHandler] Client đã kết nối: " + clientId);

      String inputLine;
      // Lắng nghe liên tục các yêu cầu (JSON) gửi từ Client
      while ((inputLine = in.readLine()) != null) {
        System.out.println("[Nhận từ " + clientId + "]: " + inputLine);

        router.route(inputLine, this);
      }

    } catch (IOException e) {
      System.out.println("[ClientHandler] Lỗi giao tiếp với client " + clientId + ": " + e.getMessage());
    } finally {
      // Đảm bảo dù lỗi hay không, khi vòng lặp kết thúc, Client phải bị xóa khỏi Server
      disconnect();
    }
  }

  /**
   * Gửi một chuỗi dữ liệu (thường là JSON) trực tiếp về Client này.
   */
  public void sendMessage(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  /**
   * Ngắt kết nối an toàn cho Client này.
   * Đóng các luồng I/O và giải phóng Socket.
   * Hàm này được để public để Server có thể chủ động gọi khi tắt hệ thống.
   */
  public void disconnect() {
    // CHIẾC KHIÊN BẢO VỆ: Nếu đã đóng rồi thì lập tức quay xe, không làm gì thêm!
    if (isClosed) {
      return;
    }
    isClosed = true; // Đánh dấu là đã đóng để các luồng sau không gọi lại nữa

    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null && !clientSocket.isClosed()) {
        clientSocket.close();
      }

      // Gỡ khỏi danh sách Server
      server.removeClient(this);
      System.out.println("[ClientHandler] Đã ngắt kết nối Client: " + clientId);
    } catch (Exception e) {
      System.err.println("[ClientHandler] Lỗi khi đóng kết nối: " + e.getMessage());
    }
  }
}