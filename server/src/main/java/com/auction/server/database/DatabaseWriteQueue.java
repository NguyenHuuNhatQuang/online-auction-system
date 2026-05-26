package com.auction.server.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Hàng đợi ghi dữ liệu đơn luồng (Single-Threaded Write Queue).
 * Đảm bảo mọi tác vụ INSERT/UPDATE vào SQLite đều xếp hàng và thực thi tuần tự,
 * loại bỏ hoàn toàn lỗi "Database is locked" và Race Condition.
 */
public class DatabaseWriteQueue {

  private static volatile DatabaseWriteQueue instance;

  // Tạo duy nhất 1 luồng công nhân (Worker Thread) để chuyên ghi DB
  private final ExecutorService writerThread;

  private DatabaseWriteQueue() {
    this.writerThread = Executors.newSingleThreadExecutor();
  }

  public static DatabaseWriteQueue getInstance() {
    if (instance == null) {
      synchronized (DatabaseWriteQueue.class) {
        if (instance == null) {
          instance = new DatabaseWriteQueue();
        }
      }
    }
    return instance;
  }

  /**
   * Nhận tác vụ ghi từ các luồng của Client và đưa vào hàng đợi.
   * Hàm này Non-blocking (trả về ngay lập tức), giúp Client không bị treo.
   */
  public void execute(Runnable task) {
    writerThread.submit(task);
  }

  /**
   * Đóng hàng đợi an toàn khi tắt Server.
   */
  public void shutdown() {
    writerThread.shutdown();
    try {
      if (!writerThread.awaitTermination(5, TimeUnit.SECONDS)) {
        writerThread.shutdownNow();
      }
    } catch (InterruptedException e) {
      writerThread.shutdownNow();
    }
  }

  /**
   * HÀM DÀNH RIÊNG CHO UNIT TEST.
   * Ép luồng chính phải chờ cho đến khi toàn bộ các lệnh trong hàng đợi được ghi xong.
   * Điều này giúp nhả file lock và đảm bảo dữ liệu đã sẵn sàng để truy vấn.
   */
  public void flushForTesting() {
    try {
      // Ném một tác vụ rỗng vào hàng đợi và dùng .get() để block luồng hiện tại
      // cho đến khi tác vụ rỗng này (và mọi tác vụ trước nó) chạy xong.
      writerThread.submit(() -> {}).get();
    } catch (Exception e) {
      // Bỏ qua lỗi trong lúc test
    }
  }
}