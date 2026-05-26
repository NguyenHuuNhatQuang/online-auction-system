package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Phiên bản "lưới thẻ" của trang Quản Lý Kho, kế thừa toàn bộ logic của
 * {@link ProductManagementController}. Chỉ override {@link #renderInventory(List)}
 * để vẽ kho dạng thẻ (card) giống mockup.
 *
 * <p>Khi click vào một thẻ, ta đặt luôn selection cho {@code itemListView} (đang ẩn),
 * nhờ đó các thao tác cũ (Đưa Lên Sàn / Cập Nhật / Xóa) vẫn hoạt động nguyên vẹn
 * mà không phải sửa gì trong lớp cha.</p>
 */
public class InventoryCardController extends ProductManagementController {

  @FXML private ScrollPane invScroll;
  @FXML private FlowPane invGrid;
  @FXML private Label invCount;

  private Node selectedCard;

  @Override
  protected void renderInventory(List<InvItem> items) {
    if (invGrid == null) {
      return; // FXML chưa nạp lưới — giữ hành vi ListView mặc định.
    }

    selectedCard = null;
    invGrid.getChildren().clear();

    if (invCount != null) {
      invCount.setText(items.size() + " sản phẩm");
    }

    // ListView vẫn được lớp cha đổ dữ liệu nhưng ta ẩn đi, chỉ dùng để giữ selection.
    itemListView.setVisible(false);
    itemListView.setManaged(false);

    if (items.isEmpty()) {
      Label empty = new Label("Kho chưa có sản phẩm nào. Hãy thêm sản phẩm đầu tiên bên dưới ⬇");
      empty.getStyleClass().add("text-muted");
      invGrid.getChildren().add(empty);
      return;
    }

    for (int i = 0; i < items.size(); i++) {
      invGrid.getChildren().add(buildCard(items.get(i), i));
    }
  }

  private Node buildCard(InvItem item, int index) {
    boolean isArt = "ART".equalsIgnoreCase(item.type);

    VBox card = new VBox();
    card.getStyleClass().add("auction-card");
    card.setPrefWidth(248);
    card.setMaxWidth(248);

    // ---- Vùng ảnh + badge loại ----
    StackPane img = new StackPane();
    img.getStyleClass().addAll("card-img", isArt ? "pic-art" : "pic-ele");

    Label icon = new Label(isArt ? "🎨" : "💻");
    icon.getStyleClass().add("card-icon");

    Label cat = new Label(item.type);
    cat.getStyleClass().addAll("badge", "badge-cat");
    StackPane.setAlignment(cat, Pos.TOP_LEFT);
    StackPane.setMargin(cat, new Insets(10, 0, 0, 10));

    img.getChildren().addAll(icon, cat);

    // ---- Thân thẻ ----
    VBox body = new VBox(3);
    body.setPadding(new Insets(12, 14, 14, 14));

    Label name = new Label(item.name);
    name.getStyleClass().add("card-title");

    String metaText = item.desc;
    if (item.attr != null && !item.attr.isBlank()) {
      metaText = item.desc + " · " + item.attr;
    }
    Label meta = new Label(metaText);
    meta.getStyleClass().add("text-muted");
    meta.setWrapText(true);

    body.getChildren().addAll(name, meta);
    card.getChildren().addAll(img, body);

    card.setOnMouseClicked(e -> selectCard(card, index));
    return card;
  }

  private void selectCard(Node card, int index) {
    // Cập nhật viền chọn
    if (selectedCard != null) {
      selectedCard.getStyleClass().remove("inv-card-selected");
    }
    if (!card.getStyleClass().contains("inv-card-selected")) {
      card.getStyleClass().add("inv-card-selected");
    }
    selectedCard = card;

    // Đặt selection cho ListView ẩn → các handler cũ dùng được ngay.
    itemListView.getSelectionModel().select(index);
  }
}
