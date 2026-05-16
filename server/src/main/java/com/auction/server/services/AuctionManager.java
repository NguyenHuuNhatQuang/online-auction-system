package com.auction.server.services;

import com.auction.common.models.Auction;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý trung tâm toàn bộ các phiên đấu giá đang hoạt động trên Server.
 * Áp dụng mẫu thiết kế Singleton để đảm bảo chỉ có duy nhất một bộ quản lý trong toàn hệ thống.
 */
public class AuctionManager {

  private static volatile AuctionManager instance;

  // Sử dụng ConcurrentHashMap để đảm bảo an toàn khi nhiều luồng (Thread) cùng thêm/đọc phiên đấu giá
  private final ConcurrentHashMap<String, Auction> activeAuctions;

  /**
   * Constructor private để ngăn chặn việc khởi tạo từ bên ngoài.
   */
  private AuctionManager() {
    activeAuctions = new ConcurrentHashMap<>();
  }

  /**
   * Lấy instance duy nhất của AuctionManager (Thread-safe Singleton).
   *
   * @return Thể hiện duy nhất của AuctionManager.
   */
  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) {
          instance = new AuctionManager();
        }
      }
    }
    return instance;
  }

  /**
   * Thêm một phiên đấu giá mới vào hệ thống quản lý.
   *
   * @param auction Đối tượng phiên đấu giá cần thêm.
   */
  public void addAuction(Auction auction) {
    if (auction != null && auction.getId() != null) {
      activeAuctions.put(auction.getId(), auction);
    }
  }

  /**
   * Lấy thông tin một phiên đấu giá dựa trên ID.
   *
   * @param auctionId ID của phiên đấu giá.
   * @return Đối tượng Auction, hoặc null nếu không tìm thấy.
   */
  public Auction getAuction(String auctionId) {
    return activeAuctions.get(auctionId);
  }

  /**
   * Lấy danh sách tất cả các phiên đấu giá đang được quản lý.
   *
   * @return Tập hợp (Collection) các phiên đấu giá.
   */
  public Collection<Auction> getAllAuctions() {
    return activeAuctions.values();
  }
}