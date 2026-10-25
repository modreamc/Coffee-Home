import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * 导出 forum 库全部表的建表 DDL 到指定文件
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." ShowSchema.java <输出文件路径>
 */
public class ShowSchema {
    public static void main(String[] args) throws Exception {
        String outPath = args.length > 0 ? args[0] : "schema.sql";
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        StringBuilder sb = new StringBuilder();
        sb.append("-- ═══ 咖啡港湾 COFFEE HARBOR · 完整建表脚本 ═══\n");
        sb.append("-- 导出时间: ").append(new java.util.Date()).append("\n");
        sb.append("CREATE DATABASE IF NOT EXISTS forum DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n");
        sb.append("USE forum;\n\n");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                String table = rs.getString(1);
                try (Statement st2 = conn.createStatement();
                     ResultSet ddl = st2.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (ddl.next()) {
                        sb.append("-- ── 表: ").append(table).append(" ──\n");
                        sb.append(ddl.getString(2)).append(";\n\n");
                    }
                }
            }
            System.out.println("✅ 已导出全部表 DDL");
        } catch (SQLException e) {
            System.err.println("❌ " + e.getMessage());
            System.exit(1);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(outPath), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        System.out.println("📄 输出文件: " + new File(outPath).getAbsolutePath());
    }
}
