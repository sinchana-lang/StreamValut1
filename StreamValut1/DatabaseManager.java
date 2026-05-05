import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/streamvault";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "sinchu151606";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void init() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id       INT PRIMARY KEY AUTO_INCREMENT,
                name     VARCHAR(100) NOT NULL,
                email    VARCHAR(100) NOT NULL UNIQUE,
                phone    VARCHAR(20),
                age      VARCHAR(10),
                password VARCHAR(100) NOT NULL
            );
        """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[db] Database ready: streamvault");
        } catch (SQLException e) {
            System.err.println("[db] Init failed: " + e.getMessage());
        }
    }
}