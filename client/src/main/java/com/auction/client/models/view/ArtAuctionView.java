package com.auction.client.models.view;

import java.time.LocalDateTime;

/**
 * View-model cho phiên đấu giá sản phẩm ART.
 * Kế thừa {@link AuctionView}, chỉ thay phần "skin" (icon + gradient tím-đỏ).
 */
public class ArtAuctionView extends AuctionView {

  protected ArtAuctionView(String id, String name, String description, double price,
                           String topBidder, String status, String itemType,
                           String sellerName, int bidCount, LocalDateTime endTime) {
    super(id, name, description, price, topBidder, status, itemType, sellerName, bidCount, endTime);
  }

  @Override
  public String getIcon() {
    return "🎨";
  }

  @Override
  public String getPicStyleClass() {
    return "pic-art";
  }

  @Override
  public String getCategoryLabel() {
    return "🎨 ART";
  }
}
