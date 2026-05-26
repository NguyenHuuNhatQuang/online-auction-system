package com.auction.client.controllers;

import com.auction.client.core.SceneManager;
import com.auction.client.models.view.AuctionView;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiên bản "Sảnh chờ" của bảng điều khiển, kế thừa toàn bộ logic mạng/lọc của
 * {@link DashboardController} và chỉ bổ sung phần hiển thị dạng lưới thẻ (card grid)
 * giống mockup. Không sửa logic cũ — chỉ override {@code render()} để rẽ nhánh:
 *
 * <ul>
 *   <li>Sảnh chờ (statusFilter = ALL) → lưới thẻ sản phẩm.</li>
 *   <li>Đang LIVE / Đã kết thúc / Đã thanh toán → giữ nguyên ListView cũ.</li>
 * </ul>
 */
public class LobbyDashboardController extends DashboardController {

  @FXML private ScrollPane lobbyScroll;
  @FXML private FlowPane lobbyGrid;
  @FXML private Label statShown;   // số phiên đang hiển thị
  @FXML private Label statLive;    // số phiên đang LIVE
  @FXML private Label filterChip;  // chip hiển thị loại sản phẩm đang lọc

  private final ObjectMapper mapper = new ObjectMapper();

  /** Các thẻ đang hiển thị cần cập nhật đồng hồ đếm ngược mỗi giây. */
  private final List<CountdownCell> countdownCells = new ArrayList<>();
  private Timeline ticker;

  private record CountdownCell(Label label, AuctionView view) { }

  @FXML
  @Override
  public void initialize() {
    super.initialize();

    // Đồng hồ đếm ngược: mỗi giây làm tươi nhãn thời gian trên các thẻ.
    ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdowns()));
    ticker.setCycleCount(Animation.INDEFINITE);
    ticker.play();
  }

  private void tickCountdowns() {
    LocalDateTime now = LocalDateTime.now();
    for (CountdownCell cell : countdownCells) {
      cell.label().setText(cell.view().getCountdownText(now));
    }
  }

  @Override
  protected void render() {
    boolean cardMode = "ALL".equals(statusFilter);

    if (lobbyScroll == null || lobbyGrid == null) {
      // Phòng hờ: nếu FXML chưa nạp được lưới thì quay về hành vi cũ.
      super.render();
      return;
    }

    if (cardMode) {
      // Chế độ Sảnh chờ: ẩn ListView, hiện lưới thẻ.
      auctionListView.setVisible(false);
      auctionListView.setManaged(false);
      lobbyScroll.setVisible(true);
      lobbyScroll.setManaged(true);
      buildCards();
      refreshNavStyles();
    } else {
      // Các filter khác: hiện ListView như cũ.
      lobbyScroll.setVisible(false);
      lobbyScroll.setManaged(false);
      auctionListView.setVisible(true);
      auctionListView.setManaged(true);
      super.render();
    }
    updateStats();
  }

  /** Dựng lại toàn bộ lưới thẻ từ dữ liệu rows (có áp dụng bộ lọc loại sản phẩm). */
  private void buildCards() {
    countdownCells.clear();
    lobbyGrid.getChildren().clear();
    LocalDateTime now = LocalDateTime.now();

    for (Row r : rows.values()) {
      if (!"ALL".equals(typeFilter)) {
        if (r.type == null || !typeFilter.equalsIgnoreCase(r.type)) continue;
      }
      lobbyGrid.getChildren().add(buildCard(toView(r), now));
    }

    if (lobbyGrid.getChildren().isEmpty()) {
      String msg = "ALL".equals(typeFilter)
          ? "Chưa có phiên đấu giá nào để hiển thị."
          : "Chưa có phiên đấu giá nào thuộc loại " + typeLabel(typeFilter) + ".";
      Label empty = new Label(msg);
      empty.getStyleClass().add("text-muted");
      lobbyGrid.getChildren().add(empty);
    }
  }

  /** Nhãn hiển thị kèm icon cho một loại sản phẩm. */
  private String typeLabel(String type) {
    if ("ELECTRONICS".equalsIgnoreCase(type)) return "📱 ELECTRONICS";
    if ("ART".equalsIgnoreCase(type)) return "🎨 ART";
    return type;
  }

  /** Chuyển Row (model nội bộ của lớp cha) sang AuctionView (model kế thừa). */
  private AuctionView toView(Row r) {
    com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
    node.put("auctionId", r.id);
    node.put("itemName", r.name);
    node.put("currentPrice", r.price);
    node.put("highestBidder", r.top);
    node.put("status", r.status);
    if (r.type != null) node.put("itemType", r.type);
    if (r.endTime != null) node.put("endTime", r.endTime);
    node.put("sellerName", r.seller != null ? r.seller : "—");
    node.put("bidCount", r.bidCount);
    return AuctionView.fromJson(node);
  }

  private Node buildCard(AuctionView v, LocalDateTime now) {
    VBox card = new VBox();
    card.getStyleClass().add("auction-card");
    card.setPrefWidth(248);
    card.setMaxWidth(248);

    // ---- Vùng ảnh (gradient theo loại) + badge ----
    StackPane img = new StackPane();
    img.getStyleClass().addAll("card-img", v.getPicStyleClass());

    Label icon = new Label(v.getIcon());
    icon.getStyleClass().add("card-icon");

    Label cat = new Label(v.getCategoryLabel());
    cat.getStyleClass().addAll("badge", "badge-cat");
    StackPane.setAlignment(cat, Pos.TOP_LEFT);
    StackPane.setMargin(cat, new Insets(10, 0, 0, 10));

    Label status = new Label(v.getStatusText());
    status.getStyleClass().addAll("badge", v.getStatusBadgeClass());
    StackPane.setAlignment(status, Pos.TOP_RIGHT);
    StackPane.setMargin(status, new Insets(10, 10, 0, 0));

    img.getChildren().addAll(icon, cat, status);

    // ---- Thân thẻ ----
    VBox body = new VBox(4);
    body.setPadding(new Insets(12, 14, 14, 14));

    Label name = new Label(v.getName());
    name.getStyleClass().add("card-title");

    Label sub = new Label(v.getSubInfo());
    sub.getStyleClass().add("text-muted");

    HBox priceRow = new HBox(10);
    priceRow.setAlignment(Pos.BOTTOM_LEFT);

    VBox priceBox = new VBox(0);
    Label priceCaption = new Label(v.getPriceLabel());
    priceCaption.getStyleClass().add("caption");
    Label price = new Label(v.getFormattedPrice());
    price.getStyleClass().add("price-sm");
    priceBox.getChildren().addAll(priceCaption, price);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label countdown = new Label(v.getCountdownText(now));
    countdown.getStyleClass().add("timer-sm");
    if (!v.isEnded()) {
      countdownCells.add(new CountdownCell(countdown, v));
    }

    priceRow.getChildren().addAll(priceBox, spacer, countdown);

    Button action = buildActionButton(v);

    body.getChildren().addAll(name, sub, priceRow, action);
    card.getChildren().addAll(img, body);

    // Click toàn thẻ = hành động chính (vào phòng nếu đang chạy).
    if (!v.isEnded()) {
      card.setOnMouseClicked(e -> openRoom(v.getId()));
    }
    return card;
  }

  private Button buildActionButton(AuctionView v) {
    Button btn = new Button();
    btn.setMaxWidth(Double.MAX_VALUE);
    VBox.setMargin(btn, new Insets(8, 0, 0, 0));

    switch (v.getStatus()) {
      case "FINISHED": {
        boolean winner = currentUser != null && currentUser.equals(v.getTopBidder());
        if (winner) {
          btn.setText("💰 Thanh toán");
          btn.getStyleClass().add("btn-success");
          btn.setOnAction(e -> payAuction(v.getId()));
        } else {
          btn.setText("Đã kết thúc");
          btn.getStyleClass().add("btn-ghost");
          btn.setDisable(true);
        }
        break;
      }
      case "PAID": {
        btn.setText("Đã thanh toán");
        btn.getStyleClass().add("btn-ghost");
        btn.setDisable(true);
        break;
      }
      case "CANCELED": {
        btn.setText("Đã hủy");
        btn.getStyleClass().add("btn-ghost");
        btn.setDisable(true);
        break;
      }
      default: {
        btn.setText("Vào phòng →");
        btn.getStyleClass().add("btn-primary");
        btn.setOnAction(e -> openRoom(v.getId()));
      }
    }
    return btn;
  }

  private void openRoom(String auctionId) {
    SceneManager.getInstance().setCurrentAuctionId(auctionId);
    SceneManager.getInstance().switchScene("/fxml/auction_room.fxml", "Phòng Đấu Giá - " + auctionId);
  }

  private void payAuction(String auctionId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
        "Bạn là người thắng! Bạn có muốn thanh toán phiên này ngay không?");
    confirm.setHeaderText(null);
    confirm.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        String request = String.format(
            "{\"action\":\"PAY_AUCTION\", \"payload\":\"{\\\"auctionId\\\":\\\"%s\\\"}\"}", auctionId);
        networkClient.sendMessage(request);
      }
    });
  }

  /** Cập nhật cụm thống kê bên phải: số phiên hiển thị & số phiên đang LIVE. */
  private void updateStats() {
    int shown = 0;
    int live = 0;
    for (Row r : rows.values()) {
      boolean matchStatus = "ALL".equals(statusFilter) || statusFilter.equals(r.status);
      boolean matchType = "ALL".equals(typeFilter)
          || (r.type != null && typeFilter.equalsIgnoreCase(r.type));
      if (matchStatus && matchType) shown++;
      if ("RUNNING".equals(r.status)) live++;
    }
    if (statShown != null) statShown.setText(String.valueOf(shown));
    if (statLive != null) statLive.setText(String.valueOf(live));

    // Chip cho biết đang lọc theo loại sản phẩm nào (ẩn khi xem tất cả).
    if (filterChip != null) {
      boolean active = !"ALL".equals(typeFilter);
      filterChip.setVisible(active);
      filterChip.setManaged(active);
      if (active) {
        filterChip.setText(typeLabel(typeFilter) + " · " + shown + " phiên");
      }
    }
  }
}
