package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class DashboardController {

  @FXML private Label welcomeLabel;
  @FXML private ListView<String> auctionListView;

  private NetworkClient networkClient;
  private String currentUser;

  @FXML
  public void initialize() {
    // 1. Lấy dữ liệu từ SceneManager
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    this.networkClient = SceneManager.getInstance().getNetworkClient();

    // 2. Cập nhật câu chào
    welcomeLabel.setText("Sảnh Chờ Đấu Giá - Xin chào " + currentUser);

    // 3. Giành quyền lắng nghe tin nhắn mạng về cho màn hình này
    networkClient.setOnMessageReceived(this::handleServerMessage);
  }

  /**
   * Xử lý tin nhắn từ Server trả về (Được gọi từ luồng mạng ngầm)
   */
  private void handleServerMessage(String jsonMessage) {
    // Tạm thời in ra console để kiểm tra
    System.out.println("[Dashboard] Nhận thông điệp: " + jsonMessage);

    // TODO: Bóc tách JSON (Jackson) để cập nhật danh sách auctionListView
    // Lưu ý: Mọi thao tác cập nhật UI đều phải bọc trong Platform.runLater()
  }

  @FXML
  private void handleCreateAuction() {
    // Gửi lệnh tạo phiên đấu giá mẫu (Giả định bán một cái Laptop)
    String payload = String.format(
        "{\"itemType\":\"ELECTRONICS\", \"itemName\":\"Laptop Gaming\", \"itemDesc\":\"Mới 100%%\", \"startPrice\":1000.0, \"durationMinutes\":10, \"sellerId\":\"%s\", \"sellerName\":\"%s\"}",
        currentUser, currentUser
    );

    String requestJson = String.format("{\"action\":\"CREATE_AUCTION\", \"payload\":%s}", escapeJson(payload));
    networkClient.sendMessage(requestJson);
  }

  @FXML
  private void handleJoinAuction() {
    String selectedItem = auctionListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Lỗi", "Vui lòng chọn một phiên đấu giá trong danh sách để tham gia!");
      return;
    }
    // TODO: Mở màn hình Phòng đấu giá
    System.out.println("Đang tham gia: " + selectedItem);
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  // Hàm phụ trợ bọc JSON payload thành chuỗi hợp lệ
  private String escapeJson(String raw) {
    return "\"" + raw.replace("\"", "\\\"") + "\"";
  }
}