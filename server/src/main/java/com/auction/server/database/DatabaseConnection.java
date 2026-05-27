package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp tiện ích chịu trách nhiệm quản lý kết nối đến SQLite.
 * Tự động tạo file database và khởi tạo cấu trúc các bảng (Tables) nếu chưa có.
 */
public class DatabaseConnection {

  // Tên file database sẽ được lưu ngay tại thư mục gốc của project
  private static final String URL = "jdbc:sqlite:auction_system.db";

  /**
   * Mở một kết nối mới đến Database.
   * Cần phải gọi conn.close() sau khi sử dụng xong (thường dùng trong khối try-with-resources).
   */
  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL);
  }

  /**
   * Khởi tạo cấu trúc cơ sở dữ liệu.
   * Lệnh "CREATE TABLE IF NOT EXISTS" đảm bảo an toàn, không ghi đè dữ liệu cũ.
   */
  public static void initDatabase() {
    String sqlCreateUsers = "CREATE TABLE IF NOT EXISTS users ("
        + "id TEXT PRIMARY KEY,"
        + "username TEXT UNIQUE NOT NULL,"
        + "password TEXT NOT NULL,"
        + "role TEXT NOT NULL"
        + ");";

    String sqlCreateItems = "CREATE TABLE IF NOT EXISTS items ("
        + "id TEXT PRIMARY KEY,"
        + "name TEXT NOT NULL,"
        + "description TEXT,"
        + "type TEXT NOT NULL,"
        + "warranty_months INTEGER DEFAULT 0,"
        + "artist TEXT DEFAULT 'Unknown',"
        + "seller_id TEXT NOT NULL,"
        + "FOREIGN KEY (seller_id) REFERENCES users(id)"
        + ");";

    String sqlCreateAuctions = "CREATE TABLE IF NOT EXISTS auctions ("
        + "id TEXT PRIMARY KEY,"
        + "item_id TEXT NOT NULL,"
        + "seller_id TEXT NOT NULL,"
        + "start_price REAL NOT NULL,"
        + "current_price REAL NOT NULL,"
        + "highest_bidder_id TEXT,"
        + "start_time TEXT NOT NULL,"
        + "end_time TEXT NOT NULL,"
        + "status TEXT NOT NULL,"
        + "FOREIGN KEY (item_id) REFERENCES items(id),"
        + "FOREIGN KEY (seller_id) REFERENCES users(id)"
        + ");";

    String sqlCreateBidTransactions = "CREATE TABLE IF NOT EXISTS bid_transactions ("
        + "id TEXT PRIMARY KEY,"
        + "auction_id TEXT NOT NULL,"
        + "bidder_id TEXT NOT NULL,"
        + "bid_amount REAL NOT NULL,"
        + "timestamp TEXT NOT NULL,"
        + "FOREIGN KEY (auction_id) REFERENCES auctions(id),"
        + "FOREIGN KEY (bidder_id) REFERENCES users(id)"
        + ");";

    // Thêm chuỗi SQL tiêm tài khoản Admin (Sử dụng 'admin' làm ID luôn cho đồng bộ)
    String sqlInsertAdmin = "INSERT OR IGNORE INTO users (id, username, password, role) VALUES ('admin', 'admin', 'admin123', 'ADMIN');";

    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(sqlCreateUsers);
      stmt.execute(sqlCreateItems);
      stmt.execute(sqlCreateAuctions);
      stmt.execute(sqlCreateBidTransactions);
      stmt.execute(sqlInsertAdmin);

      System.out.println("[Database] Đã khởi tạo cấu trúc các bảng SQLite thành công.");
    } catch (SQLException e) {
      System.err.println("[Database] Lỗi khởi tạo cơ sở dữ liệu: " + e.getMessage());
    }
  }
}