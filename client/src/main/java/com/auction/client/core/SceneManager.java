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
  private String registerRole; // Vai trò tạm thời truyền sang màn Đăng ký (BIDDER/SELLER)
  private String defaultHost = "127.0.0.1"; // Gợi ý sẵn cho màn hình Kết nối máy chủ
  private int defaultPort = 8080;

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
      Scene scene = new Scene(root, 1280, 720);
      applyTheme(scene);
      primaryStage.setTitle(title);
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      System.err.println("Lỗi khi tải giao diện " + fxmlPath + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Đường dẫn tới stylesheet BidNow dùng chung toàn app. */
  public static final String THEME_CSS = "/css/app.css";

  /** Gắn theme BidNow vào một Scene bất kỳ. */
  public void applyTheme(Scene scene) {
    java.net.URL css = getClass().getResource(THEME_CSS);
    if (css != null) {
      scene.getStylesheets().add(css.toExternalForm());
    }
  }

  // --- Getter / Setter ---
  public NetworkClient getNetworkClient() { return networkClient; }
  public void setNetworkClient(NetworkClient networkClient) { this.networkClient = networkClient; }
  public String getDefaultHost() { return defaultHost; }
  public void setDefaultHost(String defaultHost) { this.defaultHost = defaultHost; }
  public int getDefaultPort() { return defaultPort; }
  public void setDefaultPort(int defaultPort) { this.defaultPort = defaultPort; }
  public String getCurrentUser() { return currentUser; }
  public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }
  public String getCurrentAuctionId() { return currentAuctionId; }
  public void setCurrentAuctionId(String currentAuctionId) { this.currentAuctionId = currentAuctionId; }
  public String getUserRole() { return userRole; }
  public void setUserRole(String userRole) { this.userRole = userRole; }
  public String getRegisterRole() { return registerRole; }
  public void setRegisterRole(String registerRole) { this.registerRole = registerRole; }
}