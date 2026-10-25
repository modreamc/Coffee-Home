import java.sql.*;

/**
 * 修正种子帖子的发布时间（错开天数，模拟真实社区）
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." FixTime.java
 */
public class FixTime {
    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        String[][] fixes = {
            {"%塞尔达传说%", "6"}, {"%艾尔登法环%", "5"}, {"%星露谷%", "4"},
            {"%虎先锋%", "3"}, {"%独立游戏%", "2"}, {"%文明6%", "2"},
            {"%博德之门3%", "1"}, {"%只狼%", "0"}, {"%游戏荒%", "0"}, {"%极乐迪斯科%", "0"}
        };
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE posts SET created_at = DATE_SUB(NOW(), INTERVAL ? DAY), updated_at = DATE_SUB(NOW(), INTERVAL ? DAY) WHERE title LIKE ? AND category='GAME'")) {
            for (String[] f : fixes) {
                ps.setInt(1, Integer.parseInt(f[1]));
                ps.setInt(2, Integer.parseInt(f[1]));
                ps.setString(3, f[0]);
                int n = ps.executeUpdate();
                System.out.println("  " + f[0] + " => 更新 " + n + " 条, 时间 " + f[1] + " 天前");
            }
        }
        System.out.println("✅ 完成");
    }
}
