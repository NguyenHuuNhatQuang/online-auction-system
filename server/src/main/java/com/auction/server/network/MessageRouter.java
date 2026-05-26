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
import java.util.Collection;
import java.util.List;

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
        case "LOGIN":
          handleLogin(message.getPayload(), client);
          break;
        case "REGISTER": // Tính năng Đăng ký mới
          handleRegister(message.getPayload(), client);
          break;
        case "PLACE_BID":
          handlePlaceBid(message.getPayload(), client);
          break;
        case "CREATE_AUCTION":
          handleCreateAuction(message.getPayload(), client);
          break;
        case "CLIENT_DISCONNECT":
          System.out.println("[Router] Nhận yêu cầu ngắt kết nối từ Client.");
          client.disconnect();
          break;
        case "GET_ACTIVE_AUCTIONS":
          handleGetActiveAuctions(client);
          break;
        case "GET_AUCTION_STATE":
          handleGetAuctionState(message.getPayload(), client);
          break;
        case "ADD_ITEM":
          handleAddItem(message.getPayload(), client);
          break;
        case "GET_SELLER_ITEMS":
          handleGetSellerItems(message.getPayload(), client);
          break;
        case "DELETE_ITEM":
          handleDeleteItem(message.getPayload(), client);
          break;
        case "UPDATE_ITEM": // Tính năng Cập nhật sản phẩm mới
          handleUpdateItem(message.getPayload(), client);
          break;
        case "PAY_AUCTION": // Tính năng Thanh toán phiên mới
          handlePayAuction(message.getPayload(), client);
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
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String itemId = payloadNode.get("itemId").asText();
      double startPrice = payloadNode.get("startPrice").asDouble();
      int durationMinutes = payloadNode.get("durationMinutes").asInt();
      String sellerId = payloadNode.get("sellerId").asText();
      String sellerName = payloadNode.get("sellerName").asText();

      // Kiểm tra sản phẩm từ ItemManager trung tâm
      Item item = com.auction.server.services.ItemManager.getInstance().getItem(itemId);
      if (item == null) {
        throw new IllegalArgumentException("Sản phẩm không tồn tại trong kho lưu trữ.");
      }

      Seller seller = new Seller(sellerId, sellerName, "");
      LocalDateTime startTime = LocalDateTime.now();
      LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
      String auctionId = "AUC_" + UUID.randomUUID().toString().substring(0, 8);

      Auction newAuction = new Auction(auctionId, item, seller, startPrice, startTime, endTime);
      newAuction.setStatus("RUNNING");

      auctionManager.addAuction(newAuction);

      // 5. Gửi thông báo thành công cho Seller (Chỉ cần ID)
      String responsePayload = String.format("{\"auctionId\":\"%s\", \"status\":\"RUNNING\"}", auctionId);
      SocketMessage successMsg = new SocketMessage("AUCTION_CREATED", responsePayload);
      client.sendMessage(objectMapper.writeValueAsString(successMsg));

      String broadcastPayload = String.format(
          "{\"auctionId\":\"%s\", \"itemName\":\"%s\", \"itemDesc\":\"%s\", \"currentPrice\":%s, \"highestBidder\":\"Chưa có\", \"endTime\":\"%s\", \"status\":\"RUNNING\"}",
          auctionId, item.getName(), item.getDescription(), startPrice, endTime.toString()
      );
      SocketMessage broadcastMsg = new SocketMessage("NEW_AUCTION_BROADCAST", broadcastPayload);
      server.broadcastMessage(objectMapper.writeValueAsString(broadcastMsg));

      System.out.println("[MessageRouter] Đã tạo phiên đấu giá mới: " + auctionId);
    } catch (Exception e) {
      sendError(client, "Lỗi khi khởi tạo phiên đấu giá: " + e.getMessage());
    }
  }

  private void handleGetAuctionState(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String targetId = payloadNode.get("auctionId").asText();

      // Lấy phiên đấu giá từ kho (Lưu ý: Giả định kho của bạn là HashMap dùng ID làm key)
      Auction auction = AuctionManager.getInstance().getAllAuctions().stream()
          .filter(a -> targetId.equals(a.getId()))
          .findFirst()
          .orElse(null);

      if (auction != null) {
        // Xác định người đang trả giá cao nhất (nếu chưa có ai thì để là "Chưa có")
        String bidderName = "Chưa có";
        // Lưu ý: Tùy vào cách bạn thiết kế model Auction, có thể bạn lưu Bidder object hoặc tên thẳng
        // Dưới đây giả định bạn có thuộc tính highestBidder trong class Auction
        if (auction.getHighestBidder() != null) {
          bidderName = auction.getHighestBidder().getUsername();
        }

        String payload = String.format(
            "{\"auctionId\":\"%s\", \"itemName\":\"%s\", \"itemDesc\":\"%s\", \"currentPrice\":%s, \"highestBidder\":\"%s\", \"endTime\":\"%s\", \"status\":\"%s\"}",
            auction.getId(), auction.getItem().getName(), auction.getItem().getDescription(),
            auction.getCurrentPrice(), bidderName, auction.getEndTime().toString(), auction.getStatus()
        );

        String response = String.format("{\"action\":\"AUCTION_STATE\", \"payload\":%s}", escapeJson(payload));
        client.sendMessage(response);
      }
    } catch (Exception e) {
      System.err.println("[Router] Lỗi khi lấy trạng thái phiên: " + e.getMessage());
    }
  }

  private void handleGetActiveAuctions(ClientHandler client) {
    StringBuilder payload = new StringBuilder("[");
    Collection<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();

    // Lấy cả phiên RUNNING và FINISHED
    List<Auction> activeAuctions = allAuctions.stream()
        .filter(a -> "RUNNING".equals(a.getStatus())
            || "FINISHED".equals(a.getStatus())
            || "PAID".equals(a.getStatus()))
        .toList();

    for (int i = 0; i < activeAuctions.size(); i++) {
      Auction session = activeAuctions.get(i);
      String bidderName = (session.getHighestBidder() != null) ? session.getHighestBidder().getUsername() : "Chưa có";

      payload.append(String.format(
          "{\"auctionId\":\"%s\", \"itemName\":\"%s\", \"itemDesc\":\"%s\", \"currentPrice\":%s, \"highestBidder\":\"%s\", \"endTime\":\"%s\", \"status\":\"%s\"}",
          session.getId(), session.getItem().getName(), session.getItem().getDescription(),
          session.getCurrentPrice(), bidderName, session.getEndTime().toString(), session.getStatus()
      ));
      if (i < activeAuctions.size() - 1) payload.append(",");
    }
    payload.append("]");

    String response = String.format("{\"action\":\"ACTIVE_AUCTIONS_LIST\", \"payload\":%s}", escapeJson(payload.toString()));
    client.sendMessage(response);
  }

  private void handleLogin(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String username = payloadNode.get("username").asText();
      String password = payloadNode.get("password").asText();

      // Gọi UserManager để kiểm tra
      com.auction.server.services.UserManager userManager = com.auction.server.services.UserManager.getInstance();
      com.auction.common.models.User user = userManager.authenticate(username, password);

      if (user != null) {
        // Trả về thành công kèm theo Role (Vai trò) của người dùng
        String payload = String.format(
            "{\"username\":\"%s\", \"role\":\"%s\", \"userId\":\"%s\"}",
            user.getUsername(), user.getRole(), user.getId()
        );
        String response = String.format("{\"action\":\"LOGIN_SUCCESS\", \"payload\":%s}", escapeJson(payload));
        client.sendMessage(response);
      } else {
        sendError(client, "Sai tên đăng nhập hoặc mật khẩu!");
      }
    } catch (Exception e) {
      sendError(client, "Lỗi định dạng đăng nhập.");
    }
  }

  private void handleAddItem(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String itemType = payloadNode.get("itemType").asText();
      String itemName = payloadNode.get("itemName").asText();
      String itemDesc = payloadNode.get("itemDesc").asText();
      String sellerId = payloadNode.get("sellerId").asText();

      HashMap<String, Object> attributes = new HashMap<>();
      if ("ELECTRONICS".equalsIgnoreCase(itemType) && payloadNode.has("warrantyMonths")) {
        attributes.put("warrantyMonths", payloadNode.get("warrantyMonths").asInt());
      } else if ("ART".equalsIgnoreCase(itemType) && payloadNode.has("artist")) {
        attributes.put("artist", payloadNode.get("artist").asText());
      }

      Item item = ItemFactory.createItem(itemType, itemName, itemDesc, attributes);
      com.auction.server.services.ItemManager.getInstance().addItem(item, sellerId);

      sendSellerItems(sellerId, client);
    } catch (Exception e) {
      sendError(client, "Không thể thêm sản phẩm: " + e.getMessage());
    }
  }

  private void handleGetSellerItems(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String sellerId = payloadNode.get("sellerId").asText();
      sendSellerItems(sellerId, client);
    } catch (Exception e) {
      sendError(client, "Lỗi đồng bộ dữ liệu kho.");
    }
  }

  private void handleUpdateItem(String payloadJson, ClientHandler client) {
    try {
      JsonNode node = objectMapper.readTree(payloadJson);
      String itemId = node.get("itemId").asText();
      String newName = node.get("newName").asText();
      String newDesc = node.get("newDesc").asText();
      String sellerId = node.get("sellerId").asText();

      com.auction.server.services.ItemManager.getInstance().updateItem(itemId, newName, newDesc);

      sendSellerItems(sellerId, client); // Refresh lại list
    } catch (Exception e) {
      sendError(client, "Lỗi cập nhật sản phẩm.");
    }
  }

  private void handleDeleteItem(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String itemId = payloadNode.get("itemId").asText();
      String sellerId = payloadNode.get("sellerId").asText();

      com.auction.server.services.ItemManager.getInstance().deleteItem(itemId);
      sendSellerItems(sellerId, client);
    } catch (Exception e) {
      sendError(client, "Lỗi thực thi lệnh xóa.");
    }
  }

  private void sendSellerItems(String sellerId, ClientHandler client) throws JsonProcessingException {
    Collection<Item> sellerItems = com.auction.server.services.ItemManager.getInstance().getItemsBySeller(sellerId);
    StringBuilder payload = new StringBuilder("[");
    int index = 0;
    for (Item item : sellerItems) {
      String extra = "";
      if (item instanceof com.auction.common.models.Electronics) {
        extra = ", \"warrantyMonths\":" + ((com.auction.common.models.Electronics) item).getWarrantyMonths();
      } else if (item instanceof com.auction.common.models.Art) {
        extra = ", \"artist\":\"" + ((com.auction.common.models.Art) item).getArtist() + "\"";
      }
      payload.append(String.format(
          "{\"id\":\"%s\", \"name\":\"%s\", \"desc\":\"%s\", \"type\":\"%s\"%s}",
          item.getId(), item.getName(), item.getDescription(), item.getItemType(), extra
      ));
      if (++index < sellerItems.size()) {
        payload.append(",");
      }
    }
    payload.append("]");

    String response = String.format("{\"action\":\"SELLER_ITEMS_LIST\", \"payload\":%s}", escapeJson(payload.toString()));
    client.sendMessage(response);
  }

  private void handleRegister(String payloadJson, ClientHandler client) {
    try {
      JsonNode payloadNode = objectMapper.readTree(payloadJson);
      String username = payloadNode.get("username").asText();
      String password = payloadNode.get("password").asText();
      String role = payloadNode.get("role").asText();

      com.auction.common.models.User user = com.auction.server.services.UserManager.getInstance().register(username, password, role);
      if (user != null) {
        client.sendMessage("{\"action\":\"REGISTER_SUCCESS\", \"payload\":\"\"}");
      } else {
        sendError(client, "Tên đăng nhập đã tồn tại!");
      }
    } catch (Exception e) {
      sendError(client, "Lỗi định dạng đăng ký.");
    }
  }

  private void handlePayAuction(String payloadJson, ClientHandler client) {
    try {
      String auctionId = objectMapper.readTree(payloadJson).get("auctionId").asText();
      Auction auction = AuctionManager.getInstance().getAuction(auctionId);

      if (auction != null && "FINISHED".equals(auction.getStatus())) {
        auction.setStatus("PAID");

        // Broadcast cho toàn server biết phiên này đã thanh toán xong
        String payload = String.format("{\"auctionId\":\"%s\"}", auctionId);
        SocketMessage msg = new SocketMessage("AUCTION_PAID", payload);
        server.broadcastMessage(objectMapper.writeValueAsString(msg));
      }
    } catch (Exception e) {
      sendError(client, "Lỗi thanh toán.");
    }
  }

  private String escapeJson(String raw) {
    return "\"" + raw.replace("\"", "\\\"") + "\"";
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