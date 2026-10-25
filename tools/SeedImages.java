import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * 从网络下载主题图片到 picture 文件夹，并更新对应帖子的 images / content
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." SeedImages.java
 */
public class SeedImages {
    static final String PICTURE_DIR = System.getenv().getOrDefault("APP_UPLOAD_DIR", "picture");
    static final String DB_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    /** 标题关键词 -> (loremflickr 搜索标签, 本地文件名) */
    static final String[][] MAP = {
        {"塞尔达", "zelda", "zelda"},
        {"艾尔登法环", "elden ring", "eldenring"},
        {"星露谷", "stardew valley", "stardew"},
        {"虎先锋", "black myth wukong", "wukong"},
        {"独立游戏", "hollow knight", "indie"},
        {"文明6", "civilization vi", "civ6"},
        {"博德之门3", "baldurs gate", "bg3"},
        {"只狼", "sekiro", "sekiro"},
        {"游戏荒", "gaming", "gaming"},
        {"极乐迪斯科", "disco elysium", "disco"},
        {"原神牛逼", "genshin impact", "genshin"},
        {"芙莉莲", "frieren", "frieren"},
        {"进击的巨人", "attack on titan", "aot"},
        {"孤独摇滚", "bocchi the rock", "bocchi"},
        {"夏日重现", "anime mystery", "mystery"},
        {"迷宫饭", "dungeon meshi", "meshi"},
        {"补番指南", "fullmetal alchemist", "fma"},
        {"我推的孩子", "oshi no ko", "oshinoko"},
        {"新番前瞻", "anime", "newanime"},
        {"咒术回战", "jujutsu kaisen", "jjk"},
        {"追什么番", "anime girl", "animechat"},
    };

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get(PICTURE_DIR));
        int ok = 0, fail = 0;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            for (String[] m : MAP) {
                String keyword = m[0];
                String tag = m[1];
                String name = m[2];
                try {
                    String fileName = download(tag, name);
                    String url = "/picture/" + fileName;
                    // 更新帖子：images JSON + content 开头插入 <img>
                    String json = "[\"" + url + "\"]";
                    String sql = "UPDATE posts SET images = ?, content = CONCAT('<img src=\\\"', ?, '\\\" />', content) WHERE title LIKE ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, json);
                        ps.setString(2, url);
                        ps.setString(3, "%" + keyword + "%");
                        int n = ps.executeUpdate();
                        System.out.println("[OK] " + keyword + " -> " + fileName + " (updated " + n + " posts)");
                        ok++;
                    }
                } catch (Exception e) {
                    System.out.println("[FAIL] " + keyword + " : " + e.getMessage());
                    fail++;
                }
            }
            System.out.println("==== 完成: 成功 " + ok + ", 失败 " + fail + " ====");
        }
    }

    static String download(String tag, String name) throws Exception {
        // loremflickr 按标签返回相关图片（自动 302 到真实图片）
        String urlStr = "https://loremflickr.com/800/500/" + tag.replace(" ", ",");
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        int code = c.getResponseCode();
        if (code != 200) throw new RuntimeException("HTTP " + code);
        String ct = c.getContentType();
        if (ct == null || !ct.startsWith("image/")) throw new RuntimeException("not image: " + ct);
        String ext = ct.contains("png") ? ".png" : ".jpg";
        String fileName = name + ext;
        Path target = Paths.get(PICTURE_DIR).resolve(fileName);
        try (InputStream in = c.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        c.disconnect();
        if (Files.size(target) < 2000) throw new RuntimeException("file too small: " + Files.size(target));
        return fileName;
    }
}
