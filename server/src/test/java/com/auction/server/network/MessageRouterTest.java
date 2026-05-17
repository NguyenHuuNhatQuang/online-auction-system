package com.auction.server.network;

import com.auction.common.models.Auction;
import com.auction.common.models.Electronics;
import com.auction.common.models.Item;
import com.auction.common.models.Seller;
import com.auction.server.services.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Lớp kiểm thử cho MessageRouter.
 * Sử dụng Mockito để giả lập (Mock) các đối tượng liên quan đến mạng (Socket, Server).
 */
@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito cho class test này
class MessageRouterTest {

  // Tạo ra các đối tượng "thế thân" (Mock) không có logic thật, chỉ dùng để hứng dữ liệu
  @Mock
  private AuctionServer mockServer;

  @Mock
  private ClientHandler mockClient;

  private MessageRouter messageRouter;
  private AuctionManager auctionManager;

  @BeforeEach
  void setUp() {
    // Khởi tạo router thực tế, nhưng nhét cái server giả vào
    messageRouter = new MessageRouter(mockServer);

    // Chuẩn bị một phiên đấu giá giả trên hệ thống để test chức năng PLACE_BID
    auctionManager = AuctionManager.getInstance();
    auctionManager.getAllAuctions().clear(); // Dọn dẹp dữ liệu cũ

    Item item = new Electronics("item1", "Test Item", "Desc", 12);
    Seller seller = new Seller("seller1", "seller", "pass");
    Auction testAuction = new Auction("auction_test_1", item, seller, 1000.0,
        LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    testAuction.setStatus("RUNNING");

    auctionManager.addAuction(testAuction);
  }

  @Test
  @DisplayName("Định tuyến đúng khi gửi JSON lệnh PLACE_BID hợp lệ")
  void testRoute_PlaceBid_Success() {
    // 1. Chuẩn bị chuỗi JSON gửi lên từ Client
    // Lưu ý: payload bên trong phải được escape (thêm \") vì nó là 1 chuỗi JSON lồng trong chuỗi JSON
    String validJsonRequest = "{"
        + "\"action\": \"PLACE_BID\","
        + "\"payload\": \"{\\\"auctionId\\\":\\\"auction_test_1\\\", \\\"userId\\\":\\\"user1\\\", \\\"username\\\":\\\"alice\\\", \\\"amount\\\": 1500.0}\""
        + "}";

    // 2. Gọi hàm cần test
    messageRouter.route(validJsonRequest, mockClient);

    // 3. Kiểm tra (Verify) xem Router có gọi đúng các hàm phản hồi không
    // ArgumentCaptor giúp "bắt" lấy chuỗi JSON mà Router đã gửi vào hàm sendMessage của mockClient
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    // Xác nhận hàm sendMessage của client đã được gọi đúng 1 lần, hứng lấy giá trị truyền vào
    verify(mockClient).sendMessage(messageCaptor.capture());
    String responseToClient = messageCaptor.getValue();

    // Kiểm tra xem server có phát sóng (broadcast) cho mọi người không
    ArgumentCaptor<String> broadcastCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockServer).broadcastMessage(broadcastCaptor.capture());
    String broadcastMessage = broadcastCaptor.getValue();

    // 4. Khẳng định (Assert) nội dung chuỗi JSON phản hồi
    assertTrue(responseToClient.contains("BID_SUCCESS"), "Phải trả về trạng thái BID_SUCCESS cho người dùng");
    assertTrue(broadcastMessage.contains("NEW_BID_BROADCAST"), "Phải phát sóng trạng thái NEW_BID_BROADCAST");
    assertTrue(broadcastMessage.contains("1500.0"), "Gói tin phát sóng phải chứa giá mới");
  }

  @Test
  @DisplayName("Trả về ERROR khi gửi chuỗi JSON không đúng định dạng cấu trúc")
  void testRoute_InvalidJson() {
    String brokenJson = "{ \"action\": \"PLACE_BID\", thiếu ngoặc nhọn hoặc sai cú pháp";

    messageRouter.route(brokenJson, mockClient);

    // Bắt lấy tin nhắn gửi về Client
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockClient).sendMessage(messageCaptor.capture());

    assertTrue(messageCaptor.getValue().contains("ERROR"), "Phải trả về hành động ERROR");
    assertTrue(messageCaptor.getValue().contains("Sai định dạng JSON"), "Phải chứa thông báo lỗi parse JSON");
  }

  @Test
  @DisplayName("Trả về ERROR khi gửi một Action không tồn tại")
  void testRoute_UnknownAction() {
    String unknownActionJson = "{"
        + "\"action\": \"HACK_SYSTEM\","
        + "\"payload\": \"{}\""
        + "}";

    messageRouter.route(unknownActionJson, mockClient);

    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockClient).sendMessage(messageCaptor.capture());

    assertTrue(messageCaptor.getValue().contains("ERROR"));
    assertTrue(messageCaptor.getValue().contains("Hành động không được hệ thống hỗ trợ"));

    // Đảm bảo rằng server KHÔNG phát sóng bậy bạ lệnh này cho toàn hệ thống
    verify(mockServer, never()).broadcastMessage(anyString());
  }

  @Test
  @DisplayName("Định tuyến đúng và tạo phiên đấu giá khi nhận lệnh CREATE_AUCTION")
  void testRoute_CreateAuction_Success() {
    // 1. Chuẩn bị số lượng phiên đấu giá trước khi test (Đã có 1 cái từ setUp)
    int initialAuctions = auctionManager.getAllAuctions().size();

    // THÊM MỚI: Giả lập việc Seller đã thêm sản phẩm vào kho (ItemManager) trước đó
    com.auction.common.models.Item testItem = new com.auction.common.models.Electronics("item_mock_999", "Laptop Gaming", "Mới 100%", 24);
    com.auction.server.services.ItemManager.getInstance().addItem(testItem, "seller_99");

    // 2. Chuẩn bị chuỗi JSON mô phỏng lệnh tạo phiên (CẬP NHẬT: Dùng itemId thay vì itemType/itemName)
    String createAuctionJson = "{"
        + "\"action\": \"CREATE_AUCTION\","
        + "\"payload\": \"{\\\"itemId\\\":\\\"item_mock_999\\\", \\\"startPrice\\\": 1500.0, \\\"durationMinutes\\\": 60, \\\"sellerId\\\":\\\"seller_99\\\", \\\"sellerName\\\":\\\"John Doe\\\"}\""
        + "}";

    // 3. Thực thi việc định tuyến
    messageRouter.route(createAuctionJson, mockClient);

    // 4. Kiểm chứng Client nhận được phản hồi thành công
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockClient).sendMessage(messageCaptor.capture());

    String responseToClient = messageCaptor.getValue();
    assertTrue(responseToClient.contains("AUCTION_CREATED"), "Router phải báo tạo thành công");
    assertTrue(responseToClient.contains("RUNNING"), "Phiên đấu giá mới phải ở trạng thái RUNNING");

    // 5. Kiểm chứng dữ liệu trong bộ nhớ (AuctionManager) đã tăng thêm 1
    assertEquals(initialAuctions + 1, auctionManager.getAllAuctions().size(), "Hệ thống phải lưu thêm 1 phiên đấu giá mới");

    // 6. Kiểm chứng Server đã phát loa (Broadcast) báo có hàng mới lên sàn
    ArgumentCaptor<String> broadcastCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockServer).broadcastMessage(broadcastCaptor.capture());
    assertTrue(broadcastCaptor.getValue().contains("NEW_AUCTION_BROADCAST"), "Hệ thống phải phát sóng sự kiện này");
  }
}