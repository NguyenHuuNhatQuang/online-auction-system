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

  public ClientHandler(Socket clientSocket, AuctionServer server) {
    this.clientSocket = clientSocket;
    this.server = server;
    this.clientId = clientSocket.getRemoteSocketAddress().toString();
  }

  @Override
  public void run() {
    try {
      out = new PrintWriter(clientSocket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      System.out.println("[ClientHandler] Client đã kết nối: " + clientId);

      // Lắng nghe liên tục các yêu cầu (JSON) gửi từ Client
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        System.out.println("[Nhận từ " + clientId + "]: " + inputLine);

        // TODO: Chuyển đổi inputLine (JSON) thành đối tượng Request,
        // sau đó gọi BiddingService để xử lý.
      }

    } catch (IOException e) {
      System.out.println("[ClientHandler] Lỗi giao tiếp với client " + clientId + ": " + e.getMessage());
    } finally {
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
   * Dọn dẹp tài nguyên và thông báo cho Server khi Client ngắt kết nối.
   */
  public void disconnect() {
    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null && !clientSocket.isClosed()) {
        clientSocket.close();
      }
      server.removeClient(this);
      System.out.println("[ClientHandler] Client đã ngắt kết nối: " + clientId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}