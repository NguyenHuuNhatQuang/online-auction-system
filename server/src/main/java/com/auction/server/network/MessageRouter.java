package com.auction.server.network;

import com.auction.common.dto.SocketMessage;
import com.auction.common.models.Auction;
import com.auction.common.models.Bidder;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;
import com.auction.server.factories.ItemFactory;
import com.auction.server.services.AuctionManager;
import com.auction.server.services.BiddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Chịu trách nhiệm phân tích chuỗi JSON từ Client, điều hướng lệnh đến các Service tương ứng,
 * và trả về thông điệp phản hồi thích hợp.
 */
public class MessageRouter {

  private final BiddingService biddingService;
  private final ObjectMapper objectMapper;
  private final AuctionServer server;
  private final AuctionManager auctionManager;

  /**
   * Khởi tạo bộ định tuyến thông điệp.
   *
   * @param server Máy chủ Socket để gọi lệnh broadcast khi cần.
   */
  public MessageRouter(AuctionServer server) {
    this.server = server;
    this.biddingService = new BiddingService();
    this.objectMapper = new ObjectMapper();
    this.auctionManager = AuctionManager.getInstance();
  }

  /**
   * Xử lý một chuỗi JSON thô nhận được từ Client.
   *
   * @param jsonLine Chuỗi JSON do ClientHandler đọc được từ luồng mạng.
   * @param client   Đối tượng ClientHandler đại diện cho người gửi để phản hồi trực tiếp.
   */
  public void route(String jsonLine, ClientHandler client) {
    try {
      // Bước 1: Dịch chuỗi thô thành đối tượng SocketMessage
      SocketMessage message = objectMapper.readValue(jsonLine, SocketMessage.class);

      // Bước 2: Dựa vào hành động (action) để rẽ nhánh xử lý
      switch (message.getAction()) {
        case "PLACE_BID":
          handlePlaceBid(message.getPayload(), client);
          break;
        case "CREATE_AUCTION":
          handleCreateAuction(message.getPayload(), client);
          break;
        case "CLIENT_DISCONNECT": // THÊM CASE NÀY
          System.out.println("[Router] Nhận yêu cầu ngắt kết nối từ Client.");
          client.disconnect(); // Chủ động ngắt Client này ra khỏi Server
          break;
        default:
          sendError(client, "Hành động không được hệ thống hỗ trợ: " + message.getAction());
          break;
      }
    } catch (JsonProcessingException e) {
      sendError(client, "Sai định dạng JSON: " + e.getMessage());
    }
  }

  /**
   * Xử lý logic đặt giá (Bidding) khi nhận được lệnh PLACE_BID.
   */
  private void handlePlaceBid(String payloadJson, ClientHandler client) {
    try {
      // Phân tích payload (Dữ liệu gửi lên chứa: auctionId, userId, username, amount)
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String auctionId = payloadNode.get("auctionId").asText();
      String userId = payloadNode.get("userId").asText();
      String username = payloadNode.get("username").asText();
      double amount = payloadNode.get("amount").asDouble();

      // Khởi tạo đối tượng Bidder tạm thời từ dữ liệu gửi lên (Thực tế sẽ truy vấn từ DB)
      Bidder bidder = new Bidder(userId, username, "");

      // Chuyển việc xử lý nghiệp vụ cốt lõi cho BiddingService (đã có cơ chế Lock đa luồng)
      boolean success = biddingService.placeBid(auctionId, bidder, amount);

      if (success) {
        // 1. Gửi thông báo thành công cho RIÊNG người đặt giá
        SocketMessage successMsg = new SocketMessage("BID_SUCCESS", "Bạn đã đặt giá thành công.");
        client.sendMessage(objectMapper.writeValueAsString(successMsg));

        // 2. REALTIME UPDATE: Phát sóng giá mới cho TẤT CẢ mọi người đang online
        String broadcastPayload = String.format(
            "{\"auctionId\":\"%s\", \"newPrice\":%f, \"highestBidder\":\"%s\"}",
            auctionId, amount, username
        );
        SocketMessage broadcastMsg = new SocketMessage("NEW_BID_BROADCAST", broadcastPayload);
        server.broadcastMessage(objectMapper.writeValueAsString(broadcastMsg));
      }

    } catch (Exception e) {
      // Bắt các lỗi nghiệp vụ từ BiddingService (Ví dụ: "Giá đặt phải cao hơn hiện tại")
      // và báo lỗi về CỤ THỂ cho người vừa đặt sai, không broadcast lỗi này cho người khác.
      sendError(client, e.getMessage());
    }
  }

  /**
   * Xử lý logic tạo phiên đấu giá mới khi nhận được lệnh CREATE_AUCTION.
   * Người gửi (Seller) truyền thông tin sản phẩm và thời gian đấu giá.
   */
  private void handleCreateAuction(String payloadJson, ClientHandler client) {
    try {
      // 1. Phân tích dữ liệu sản phẩm từ JSON
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String itemType = payloadNode.get("itemType").asText(); // "ELECTRONICS" hoặc "ART"
      String itemName = payloadNode.get("itemName").asText();
      String itemDesc = payloadNode.get("itemDesc").asText();
      double startPrice = payloadNode.get("startPrice").asDouble();
      int durationMinutes = payloadNode.get("durationMinutes").asInt(); // Thời lượng phiên (phút)

      // Thông tin người bán (Thực tế sẽ lấy từ Token hoặc Session đăng nhập)
      String sellerId = payloadNode.get("sellerId").asText();
      String sellerName = payloadNode.get("sellerName").asText();

      // 2. Sử dụng Factory để tạo Sản phẩm
      Item item = ItemFactory.createItem(itemType, itemName, itemDesc, new HashMap<>());

      // 3. Khởi tạo Phiên đấu giá
      Seller seller = new Seller(sellerId, sellerName, "");
      LocalDateTime startTime = LocalDateTime.now();
      LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
      String auctionId = "AUC_" + UUID.randomUUID().toString().substring(0, 8);

      Auction newAuction = new Auction(auctionId, item, seller, startPrice, startTime, endTime);
      newAuction.setStatus("RUNNING");

      // 4. Lưu vào hệ thống quản lý trung tâm
      auctionManager.addAuction(newAuction);

      // 5. Gửi thông báo thành công cho Seller
      String responsePayload = String.format("{\"auctionId\":\"%s\", \"status\":\"RUNNING\"}", auctionId);
      SocketMessage successMsg = new SocketMessage("AUCTION_CREATED", responsePayload);
      client.sendMessage(objectMapper.writeValueAsString(successMsg));

      // (Tùy chọn) Phát sóng cho mọi người biết có hàng mới lên sàn
      SocketMessage broadcastMsg = new SocketMessage("NEW_AUCTION_BROADCAST", responsePayload);
      server.broadcastMessage(objectMapper.writeValueAsString(broadcastMsg));

      System.out.println("[MessageRouter] Đã tạo phiên đấu giá mới: " + auctionId);

    } catch (Exception e) {
      sendError(client, "Lỗi khi tạo phiên đấu giá: " + e.getMessage());
    }
  }

  /**
   * Hàm tiện ích để đóng gói và gửi thông báo lỗi về cho Client.
   */
  private void sendError(ClientHandler client, String errorMessage) {
    try {
      SocketMessage errorMsg = new SocketMessage("ERROR", errorMessage);
      client.sendMessage(objectMapper.writeValueAsString(errorMsg));
    } catch (JsonProcessingException e) {
      e.printStackTrace(); // Log lỗi server nếu việc tạo JSON lỗi bị hỏng
    }
  }
}