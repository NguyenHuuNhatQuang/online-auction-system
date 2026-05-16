package com.auction.common.models;

/**
 * Đại diện cho tác phẩm nghệ thuật.
 * Bổ sung thêm thuộc tính tác giả (nghệ sĩ).
 */
public class Art extends Item {

  private String artist;

  /**
   * Khởi tạo tác phẩm nghệ thuật.
   *
   * @param id          Định danh tác phẩm.
   * @param name        Tên tác phẩm.
   * @param description Mô tả.
   * @param artist      Tên nghệ sĩ/tác giả.
   */
  public Art(String id, String name, String description, String artist) {
    super(id, name, description);
    this.artist = artist;
  }

  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  @Override
  public String getItemType() {
    return "ART";
  }
}