import java.sql.*;

/**
 * 咖啡港湾数据库连接检查工具
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." DbCheck.java
 * 连接参数与 application.yml 一致
 */
public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = conn.getMetaData();
            System.out.println("✅ 连接成功: " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("   数据库: " + conn.getCatalog() + " / 用户: " + user);
            System.out.println("---- 表清单 ----");
            try (ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME");
                    long count = 0;
                    try (Statement st = conn.createStatement();
                         ResultSet cr = st.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                        if (cr.next()) count = cr.getLong(1);
                    } catch (SQLException e) { count = -1; }
                    System.out.printf("  %-22s 行数: %d%n", table, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
