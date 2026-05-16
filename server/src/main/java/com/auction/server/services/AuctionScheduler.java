package com.auction.server.services;

import com.auction.common.dto.SocketMessage;
import com.auction.common.models.Auction;
import com.auction.common.models.Bidder;
import com.auction.server.network.AuctionServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bộ định thời chạy ngầm của Server.
 * Chịu trách nhiệm quét định kỳ để đóng các phiên đấu giá đã hết hạn
 * và phát sóng (broadcast) kết quả cho toàn bộ người dùng.
 */
public class AuctionScheduler {

  // Trình quản lý luồng định kỳ (Chỉ cần 1 luồng chạy ngầm là đủ)
  private final ScheduledExecutorService scheduler;
  private final AuctionManager auctionManager;
  private final BiddingService biddingService;
  private final AuctionServer server;
  private final ObjectMapper objectMapper;

  /**
   * Khởi tạo bộ định thời.
   *
   * @param biddingService Dịch vụ xử lý khóa và logic đấu giá.
   * @param server         Máy chủ Socket để phát sóng thông báo.
   */
  public AuctionScheduler(BiddingService biddingService, AuctionServer server) {
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    this.auctionManager = AuctionManager.getInstance();
    this.biddingService = biddingService;
    this.server = server;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Bắt đầu tiến trình chạy ngầm. Quét hệ thống mỗi 1 giây.
   */
  public void start() {
    System.out.println("[AuctionScheduler] Đã khởi động bộ quét thời gian ngầm.");
    // Thực thi hàm scanExpiredAuctions() sau mỗi 1 giây (độ trễ ban đầu 1s)
    scheduler.scheduleAtFixedRate(this::scanExpiredAuctions, 1, 1, TimeUnit.SECONDS);
  }

  /**
   * Dừng tiến trình chạy ngầm khi tắt Server.
   */
  public void stop() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      System.out.println("[AuctionScheduler] Đã tắt bộ quét thời gian ngầm.");
    }
  }

  /**
   * Quét qua toàn bộ danh sách phiên đấu giá và đóng các phiên hết hạn.
   */
  private void scanExpiredAuctions() {
    for (Auction auction : auctionManager.getAllAuctions()) {
      // Gọi hàm check an toàn từ BiddingService
      Auction closedAuction = biddingService.checkAndCloseIfExpired(auction.getId());

      // Nếu phát hiện có phiên vừa bị đóng
      if (closedAuction != null) {
        System.out.println("[AuctionScheduler] Phiên " + closedAuction.getId() + " đã kết thúc!");
        broadcastAuctionFinished(closedAuction);
      }
    }
  }

  /**
   * Tạo và phát sóng gói tin JSON thông báo kết quả phiên đấu giá.
   */
  private void broadcastAuctionFinished(Auction auction) {
    try {
      Bidder winner = auction.getHighestBidder();
      String winnerName = (winner != null) ? winner.getUsername() : "Không có ai";

      // Tạo chuỗi JSON Payload chứa thông tin người thắng cuộc
      String payload = String.format(
          "{\"auctionId\":\"%s\", \"winner\":\"%s\", \"finalPrice\":%f}",
          auction.getId(), winnerName, auction.getCurrentPrice()
      );

      // Gói vào DTO chuẩn của hệ thống
      SocketMessage message = new SocketMessage("AUCTION_FINISHED", payload);

      // Ép thành chuỗi JSON và nhờ Server phát loa cho toàn hệ thống
      server.broadcastMessage(objectMapper.writeValueAsString(message));

    } catch (JsonProcessingException e) {
      System.err.println("[AuctionScheduler] Lỗi tạo JSON thông báo kết thúc: " + e.getMessage());
    }
  }
}