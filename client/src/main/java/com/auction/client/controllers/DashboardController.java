package com.auction.client.controllers;

import com.auction.common.dto.SocketMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auction.client.core.SceneManager;
import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardController {

  @FXML private Label welcomeLabel;
  @FXML protected ListView<String> auctionListView;

  @FXML private Button createAuctionBtn;
  @FXML private Button wizardBtn;
  @FXML private Button adminPanelBtn;

  // Sidebar nav (lọc danh sách phiên)
  @FXML private Label navLobby;
  @FXML private Label navLive;
  @FXML private Label navFinished;
  @FXML private Label navPaid;
  @FXML private Label navElectronics;
  @FXML private Label navArt;

  protected NetworkClient networkClient;
  protected String currentUser;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Backing model + trạng thái lọc
  protected static class Row {
    String id;
    String name;
    String status;  // RUNNING | FINISHED | PAID
    String type;    // ELECTRONICS | ART | null
    String top;
    double price;
    String endTime;     // ISO LocalDateTime (rỗng nếu không có)
    String seller;      // tên seller
    int bidCount;       // số lượt bid
  }

  protected final Map<String, Row> rows = new LinkedHashMap<>();
  protected String statusFilter = "ALL"; // ALL | RUNNING | FINISHED | PAID
  protected String typeFilter = "ALL";   // ALL | ELECTRONICS | ART

  @FXML
  public void initialize() {
    this.currentUser = SceneManager.getInstance().getCurrentUser();
    String userRole = SceneManager.getInstance().getUserRole();
    this.networkClient = SceneManager.getInstance().getNetworkClient();
    welcomeLabel.setText("Sảnh Chờ Đấu Giá - Xin chào " + currentUser + " (" + userRole + ")");
    networkClient.setOnMessageReceived(this::handleServerMessage);

    // Xử lý hiện/ẩn nút dựa trên quyền
    if ("BIDDER".equalsIgnoreCase(userRole)) {
      createAuctionBtn.setVisible(false);
      createAuctionBtn.setManaged(false);
      if (wizardBtn != null) {
        wizardBtn.setVisible(false);
        wizardBtn.setManaged(false);
      }
    }
    // Nếu là Admin, hiện nút Bảng Quản Trị (Admin vẫn được phép dùng Kho Sản Phẩm)
    if ("ADMIN".equalsIgnoreCase(userRole)) {
      adminPanelBtn.setVisible(true);
      adminPanelBtn.setManaged(true);
    }

    refreshNavStyles();
    networkClient.sendMessage("{\"action\":\"GET_ACTIVE_AUCTIONS\", \"payload\":\"\"}");
  }

  // ---------------- Sidebar filters ----------------

  @FXML private void filterLobby() { statusFilter = "ALL"; typeFilter = "ALL"; render(); }
  @FXML private void filterLive() { statusFilter = "RUNNING"; render(); }
  // "Đã kết thúc" = lịch sử mọi phiên đã đóng (hết giờ, đã thanh toán, đã hủy).
  @FXML private void filterFinished() { statusFilter = "ENDED"; render(); }
  @FXML private void filterPaid() { statusFilter = "PAID"; render(); }

  @FXML
  private void filterElectronics() {
    typeFilter = "ELECTRONICS".equals(typeFilter) ? "ALL" : "ELECTRONICS";
    // Lọc theo loại thì luôn về "Sảnh chờ" để hiện mọi trạng thái của loại đó.
    statusFilter = "ALL";
    render();
  }

  @FXML
  private void filterArt() {
    typeFilter = "ART".equals(typeFilter) ? "ALL" : "ART";
    statusFilter = "ALL";
    render();
  }

  /** Khớp trạng thái theo bộ lọc hiện tại. "ENDED" gộp mọi phiên đã đóng. */
  protected boolean matchesStatus(Row r) {
    switch (statusFilter) {
      case "ALL":
        return true;
      case "ENDED":
        return "FINISHED".equals(r.status) || "PAID".equals(r.status) || "CANCELED".equals(r.status);
      default:
        return statusFilter.equals(r.status);
    }
  }

  protected void render() {
    String selected = auctionListView.getSelectionModel().getSelectedItem();
    auctionListView.getItems().clear();
    for (Row r : rows.values()) {
      if (!matchesStatus(r)) continue;
      if (!"ALL".equals(typeFilter)) {
        if (r.type == null || !typeFilter.equalsIgnoreCase(r.type)) continue;
      }
      auctionListView.getItems().add(displayOf(r));
    }
    if (selected != null && auctionListView.getItems().contains(selected)) {
      auctionListView.getSelectionModel().select(selected);
    }
    refreshNavStyles();
  }

  protected String displayOf(Row r) {
    boolean ended = "FINISHED".equals(r.status) || "PAID".equals(r.status) || "CANCELED".equals(r.status);
    String statusText;
    switch (r.status) {
      case "FINISHED": statusText = "[KẾT THÚC]"; break;
      case "PAID": statusText = "[ĐÃ THANH TOÁN]"; break;
      case "CANCELED": statusText = "[ĐÃ HỦY]"; break;
      default: statusText = "[ĐANG CHẠY]";
    }
    String topPart = ended ? r.top + " (WIN)" : r.top;
    String pricePart = ended
        ? String.format("Giá Chốt: %,.0f VND", r.price)
        : String.format("Giá: %,.0f VND", r.price);
    return String.format("%s %s | %s | Top: %s | %s", statusText, r.id, r.name, topPart, pricePart);
  }

  protected void refreshNavStyles() {
    styleNav(navLobby, "ALL".equals(statusFilter) && "ALL".equals(typeFilter));
    styleNav(navLive, "RUNNING".equals(statusFilter));
    styleNav(navFinished, "ENDED".equals(statusFilter));
    styleNav(navPaid, "PAID".equals(statusFilter));
    styleNav(navElectronics, "ELECTRONICS".equals(typeFilter));
    styleNav(navArt, "ART".equals(typeFilter));
  }

  private void styleNav(Label label, boolean active) {
    if (label == null) return;
    label.getStyleClass().remove("nav-item-active");
    if (active) label.getStyleClass().add("nav-item-active");
  }

  /**
   * Xử lý tin nhắn từ Server trả về (Được gọi từ luồng mạng ngầm)
   */
  private void handleServerMessage(String jsonMessage) {
    System.out.println("[Dashboard] Nhận thông điệp: " + jsonMessage);

    try {
      SocketMessage message = objectMapper.readValue(jsonMessage, SocketMessage.class);

      Platform.runLater(() -> {
        try {
          switch (message.getAction()) {
            case "ACTIVE_AUCTIONS_LIST": {
              JsonNode arrayNode = objectMapper.readTree(message.getPayload());
              rows.clear();
              for (JsonNode node : arrayNode) {
                Row r = new Row();
                r.id = node.get("auctionId").asText();
                r.name = node.get("itemName").asText();
                r.price = node.get("currentPrice").asDouble();
                r.top = node.get("highestBidder").asText();
                r.status = node.get("status").asText();
                r.type = node.has("itemType") ? node.get("itemType").asText() : null;
                r.endTime = node.has("endTime") ? node.get("endTime").asText() : null;
                r.seller = node.has("sellerName") ? node.get("sellerName").asText() : "—";
                r.bidCount = node.has("bidCount") ? node.get("bidCount").asInt() : 0;
                rows.put(r.id, r);
              }
              render();
              break;
            }

            case "NEW_AUCTION_BROADCAST": {
              JsonNode node = objectMapper.readTree(message.getPayload());
              Row r = new Row();
              r.id = node.get("auctionId").asText();
              r.name = node.get("itemName").asText();
              r.price = node.get("currentPrice").asDouble();
              r.top = "Chưa có";
              r.status = "RUNNING";
              r.type = node.has("itemType") ? node.get("itemType").asText() : null;
              r.endTime = node.has("endTime") ? node.get("endTime").asText() : null;
              r.seller = node.has("sellerName") ? node.get("sellerName").asText() : "—";
              r.bidCount = node.has("bidCount") ? node.get("bidCount").asInt() : 0;
              rows.put(r.id, r);
              render();
              break;
            }

            case "NEW_BID_BROADCAST": {
              JsonNode bidNode = objectMapper.readTree(message.getPayload());
              Row r = rows.get(bidNode.get("auctionId").asText());
              if (r != null) {
                r.price = bidNode.get("newPrice").asDouble();
                r.top = bidNode.get("highestBidder").asText();
                r.bidCount++;
                render();
              }
              break;
            }

            case "AUCTION_FINISHED": {
              JsonNode endNode = objectMapper.readTree(message.getPayload());
              Row r = rows.get(endNode.get("auctionId").asText());
              if (r != null) {
                r.status = "FINISHED";
                r.top = endNode.get("winner").asText();
                r.price = endNode.get("finalPrice").asDouble();
                render();
              }
              break;
            }

            case "AUCTION_PAID": {
              JsonNode paidNode = objectMapper.readTree(message.getPayload());
              Row r = rows.get(paidNode.get("auctionId").asText());
              if (r != null) {
                r.status = "PAID";
                render();
              }
              break;
            }

            case "SERVER_DISCONNECTED": {
              showAlert("Mất kết nối", "Máy chủ đã dừng hoạt động. Vui lòng thoát ứng dụng.");
              break;
            }

            case "ERROR": {
              showAlert("Lỗi hệ thống", message.getPayload());
              break;
            }
          }
        } catch (Exception e) {
          System.err.println("Lỗi xử lý payload: " + e.getMessage());
        }
      });

    } catch (Exception e) {
      System.err.println("Không thể parse JSON: " + jsonMessage);
    }
  }

  @FXML
  private void handleGoToManageProducts() {
    SceneManager.getInstance().switchScene("/fxml/product_management.fxml", "Kho hàng của tôi - " + currentUser);
  }

  @FXML
  private void handleGoToWizard() {
    SceneManager.getInstance().switchScene("/fxml/listing_wizard.fxml", "Đăng sản phẩm mới - " + currentUser);
  }

  @FXML
  private void handleJoinAuction() {
    String selectedItem = auctionListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) return;

    String[] parts = selectedItem.split("\\|");
    String auctionId = parts[0].replaceAll("\\[.*?\\]", "").trim(); // Lọc bỏ chữ [ĐANG CHẠY] hoặc [KẾT THÚC]

    // Nếu phiên đã thanh toán
    if (selectedItem.startsWith("[ĐÃ THANH TOÁN]")) {
      showAlert("Thông báo", "Sản phẩm này đã được thanh toán xong.");
      return;
    }

    // Nếu phiên kết thúc nhưng chưa thanh toán
    if (selectedItem.startsWith("[KẾT THÚC]")) {
      // SỬA TẠI ĐÂY: Chỉ cần kiểm tra xem tên mình có nằm ở phần Top hay không
      if (selectedItem.contains("Top: " + currentUser)) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn là người thắng! Bạn có muốn thanh toán phiên này ngay không?");
        confirm.showAndWait().ifPresent(response -> {
          if (response == javafx.scene.control.ButtonType.OK) {
            String request = String.format("{\"action\":\"PAY_AUCTION\", \"payload\":\"{\\\"auctionId\\\":\\\"%s\\\"}\"}", auctionId);
            networkClient.sendMessage(request);
          }
        });
      } else {
        showAlert("Từ chối", "Phiên đấu giá đã kết thúc. Bạn không phải là người chiến thắng.");
      }
      return;
    }

    // Nếu đang chạy thì vào phòng bình thường
    SceneManager.getInstance().setCurrentAuctionId(auctionId);
    SceneManager.getInstance().switchScene("/fxml/auction_room.fxml", "Phòng Đấu Giá - " + auctionId);
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  @FXML
  private void handleLogout() {
    SceneManager.getInstance().setCurrentUser(null);
    SceneManager.getInstance().setUserRole(null);
    SceneManager.getInstance().switchScene("/fxml/login.fxml", "Đăng nhập Sàn Đấu Giá");
  }

  @FXML
  private void handleGoToAdminPanel() {
    SceneManager.getInstance().switchScene("/fxml/admin_dashboard.fxml", "Bảng Điều Khiển Quản Trị Viên");
  }
}
