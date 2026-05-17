package com.auction.server.services;

import com.auction.common.models.Item;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Quản lý kho lưu trữ sản phẩm thô của hệ thống.
 * Đảm bảo an toàn đa luồng và dễ dàng thay thế bằng JDBC DAO về sau.
 */
public class ItemManager {

  private static volatile ItemManager instance;
  private final ConcurrentHashMap<String, Item> items;
  private final ConcurrentHashMap<String, String> itemOwners; // itemId -> sellerId

  private ItemManager() {
    this.items = new ConcurrentHashMap<>();
    this.itemOwners = new ConcurrentHashMap<>();
  }

  public static ItemManager getInstance() {
    if (instance == null) {
      synchronized (ItemManager.class) {
        if (instance == null) {
          instance = new ItemManager();
        }
      }
    }
    return instance;
  }

  public void addItem(Item item, String sellerId) {
    items.put(item.getId(), item);
    itemOwners.put(item.getId(), sellerId);
  }

  public Item getItem(String itemId) {
    return items.get(itemId);
  }

  public void deleteItem(String itemId) {
    items.remove(itemId);
    itemOwners.remove(itemId);
  }

  public Collection<Item> getItemsBySeller(String sellerId) {
    return items.entrySet().stream()
        .filter(entry -> sellerId.equals(itemOwners.get(entry.getKey())))
        .map(ConcurrentHashMap.Entry::getValue)
        .collect(Collectors.toList());
  }
}