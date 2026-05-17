package com.auction.client.core;

import com.auction.client.network.NetworkClient;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Quản lý việc chuyển đổi giữa các màn hình (Scene) và lưu trữ dữ liệu phiên (Session).
 * Thiết kế tối giản: Giữ NetworkClient và Tên người dùng để dùng chung toàn cục.
 */
public class SceneManager {

  private static SceneManager instance;
  private Stage primaryStage;
  private NetworkClient networkClient;
  private String currentUser; // Lưu tên người dùng sau khi nhập
  private String currentAuctionId;
  private String userRole;

  private SceneManager() {}

  public static SceneManager getInstance() {
    if (instance == null) {
      instance = new SceneManager();
    }
    return instance;
  }

  public void init(Stage stage, NetworkClient client) {
    this.primaryStage = stage;
    this.networkClient = client;
  }

  /**
   * Hàm dùng chung để tải file FXML và chuyển màn hình.
   */
  public void switchScene(String fxmlPath, String title) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent root = loader.load();
      Scene scene = new Scene(root, 600, 400); // Cố định kích thước cửa sổ tối giản
      primaryStage.setTitle(title);
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      System.err.println("Lỗi khi tải giao diện " + fxmlPath + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  // --- Getter / Setter ---
  public NetworkClient getNetworkClient() { return networkClient; }
  public String getCurrentUser() { return currentUser; }
  public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }
  public String getCurrentAuctionId() { return currentAuctionId; }
  public void setCurrentAuctionId(String currentAuctionId) { this.currentAuctionId = currentAuctionId; }
  public String getUserRole() { return userRole; }
  public void setUserRole(String userRole) { this.userRole = userRole; }
}