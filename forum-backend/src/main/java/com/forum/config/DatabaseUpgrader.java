package com.forum.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
/**
 * 启动时做幂等升级（当前项目的"迁移脚本"）。
 * 注意：这是单实例方案；多实例或正式环境建议换成 Flyway/Liquibase。
 */
@Component
public class DatabaseUpgrader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUpgrader.class);
    private final DataSource dataSource;
    public DatabaseUpgrader(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS tags (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(50) NOT NULL UNIQUE, " +
                    "post_count INT NOT NULL DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.execute("CREATE TABLE IF NOT EXISTS sign_ins (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id BIGINT NOT NULL, " +
                    "sign_date DATE NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY uk_user_date (user_id, sign_date)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.execute("CREATE TABLE IF NOT EXISTS announcements (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "title VARCHAR(100) NOT NULL, " +
                    "content TEXT, " +
                    "is_active TINYINT(1) NOT NULL DEFAULT 1, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id BIGINT NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "content VARCHAR(255) NOT NULL, " +
                    "related_id BIGINT DEFAULT NULL, " +
                    "is_read TINYINT(1) NOT NULL DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "KEY idx_user (user_id, is_read)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            ensureColumn(conn, st, "posts", "is_top", "TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumn(conn, st, "posts", "is_essence", "TINYINT(1) NOT NULL DEFAULT 0");
            ensureColumn(conn, st, "posts", "tags", "VARCHAR(255) DEFAULT NULL");
            ensureColumn(conn, st, "posts", "videos", "TEXT DEFAULT NULL COMMENT '视频链接 JSON 数组'");
            ensureColumn(conn, st, "users", "points", "INT NOT NULL DEFAULT 0");
            ensureColumn(conn, st, "comments", "parent_id", "BIGINT DEFAULT NULL COMMENT '回复的父评论ID，可为NULL'");

            // 老数据可能为 NULL，统一收敛，避免计数更新 NPE
            st.execute("UPDATE posts SET like_count = 0 WHERE like_count IS NULL");
            st.execute("UPDATE posts SET collect_count = 0 WHERE collect_count IS NULL");
            st.execute("UPDATE posts SET comment_count = 0 WHERE comment_count IS NULL");
            st.execute("UPDATE users SET points = 0 WHERE points IS NULL");
            log.info("database schema check finished");
        }
    }

    private void ensureColumn(Connection conn, Statement st, String table, String column, String ddl) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        boolean exists = false;
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            exists = rs.next();
        }
        if (!exists) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
            log.info("db upgrade: added column {}.{}", table, column);
        }
    }
}
