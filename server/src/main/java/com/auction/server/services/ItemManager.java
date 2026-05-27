package com.auction.server.services;

import com.auction.common.models.Item;
import com.auction.server.database.DatabaseWriteQueue;
import com.auction.server.database.ItemDAO;
import java.util.Collection;

public class ItemManager {

  private static volatile ItemManager instance;
  private final ItemDAO itemDAO;

  private ItemManager() {
    this.itemDAO = new ItemDAO();
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
    DatabaseWriteQueue.getInstance().execute(() -> {
      try {
        itemDAO.insertItem(item, sellerId);
      } catch (Exception e) {
        System.err.println("[ItemManager] Lỗi lưu sản phẩm: " + e.getMessage());
      }
    });
  }

  public Item getItem(String itemId) {
    return itemDAO.getItem(itemId);
  }

  public void deleteItem(String itemId) {
    DatabaseWriteQueue.getInstance().execute(() -> {
      try {
        itemDAO.deleteItem(itemId);
      } catch (Exception e) {
        System.err.println("[ItemManager] Lỗi xóa sản phẩm: " + e.getMessage());
      }
    });
  }

  public void updateItem(String itemId, String newName, String newDesc) {
    DatabaseWriteQueue.getInstance().execute(() -> {
      try {
        itemDAO.updateItem(itemId, newName, newDesc);
      } catch (Exception e) {
        System.err.println("[ItemManager] Lỗi cập nhật sản phẩm: " + e.getMessage());
      }
    });
  }

  public Collection<Item> getItemsBySeller(String sellerId) {
    return itemDAO.getItemsBySeller(sellerId);
  }

  public Collection<Item> getAllItems() {
    return itemDAO.getAllItems();
  }
}