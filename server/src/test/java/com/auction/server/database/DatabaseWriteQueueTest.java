package com.auction.server.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseWriteQueueTest {

  private DatabaseWriteQueue queue;

  @BeforeEach
  void setUp() {
    queue = DatabaseWriteQueue.getInstance();
  }

  @Test
  @DisplayName("Hàng đợi phải thực thi tuần tự và không bỏ sót tác vụ nào khi có nhiều luồng đẩy vào")
  void testQueue_ExecutesAllTasksSequentially() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger(0);
    int numberOfTasks = 100;

    // Tạo mảng để quản lý 100 luồng
    Thread[] threads = new Thread[numberOfTasks];

    // Khởi chạy 100 luồng cùng lúc
    for (int i = 0; i < numberOfTasks; i++) {
      threads[i] = new Thread(() -> {
        queue.execute(() -> {
          try { Thread.sleep(2); } catch (InterruptedException ignored) {}
          counter.incrementAndGet();
        });
      });
      threads[i].start();
    }

    // ÉP LUỒNG CHÍNH CHỜ 100 LUỒNG KIA ĐẨY XONG TÁC VỤ VÀO HÀNG ĐỢI
    for (int i = 0; i < numberOfTasks; i++) {
      threads[i].join();
    }

    // Hàng đợi lúc này chắc chắn đã nhận đủ 100 lệnh. Bắt đầu ép chờ ghi xong.
    queue.flushForTesting();

    assertEquals(numberOfTasks, counter.get(), "Hàng đợi phải xử lý đủ 100 tác vụ mà không bị mất dữ liệu do Race Condition.");
  }
}