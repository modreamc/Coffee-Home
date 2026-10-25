import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量内容种子：把 tools/seed-data/*.txt 里的长文帖子写入数据库，
 * 并从 Steam / Kitsu / Bing 图搜下载配图到后端 picture 目录。
 *
 * 用法：
 *   java -cp "<mysql-connector-j.jar>" tools/SeedContent.java
 * 可选环境变量：
 *   DB_URL / DB_USER / DB_PASSWORD / APP_UPLOAD_DIR
 */
public class SeedContent {

    record Post(String title, String cat, List<String> tags, String author, int days,
                String steam, String kitsu, String imgq, String videos, String body) {}

    static final String DB_URL = env("DB_URL", "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    static final String DB_USER = env("DB_USER", "root");
    static final String DB_PASS = env("DB_PASSWORD", "");
    static final Path IMG_DIR = Paths.get(env("APP_UPLOAD_DIR", "picture")).toAbsolutePath().normalize();
    static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    static int imgOk = 0, imgFail = 0, inserted = 0, updated = 0;
    /** 同一款游戏的第二、三篇帖子换用不同的官方素材，避免列表里出现重复封面。 */
    static final String[] GAME_ASSETS = {"library_hero.jpg", "capsule_616x353.jpg", "header.jpg"};
    static final Map<String, Integer> ASSET_USE = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "tools/seed-data");
        List<Post> posts = loadPosts(dataDir);
        System.out.println("[seed] parsed posts: " + posts.size());
        Files.createDirectories(IMG_DIR);

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            ensureVideosColumn(conn);
            Map<String, User> users = loadUsers(conn);
            List<Long> ids = new ArrayList<>();
            for (Post p : posts) {
                String imgUrl = resolveImage(p);
                long id = upsert(conn, p, imgUrl, users);
                ids.add(id);
            }
            rebuildTags(conn);
            seedSocial(conn, ids, users);
            recomputeCounts(conn);
            conn.commit();
        }
        System.out.println("[seed] inserted=" + inserted + ", updated=" + updated
                + ", images ok=" + imgOk + ", images failed=" + imgFail);
    }

    /* ── 解析种子文件 ─────────────────────────────────────────── */

    static List<Post> loadPosts(Path dir) throws IOException {
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".txt")).sorted().toList();
        }
        List<Post> posts = new ArrayList<>();
        for (Path f : files) {
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            String title = null, cat = null, author = "testuser", steam = null, kitsu = null, imgq = null, videos = null;
            int days = 7;
            List<String> tags = List.of();
            StringBuilder body = new StringBuilder();
            boolean inBody = false;
            for (String raw : lines) {
                String line = raw;
                if (line.equals("### POST")) {
                    if (title != null) {
                        posts.add(new Post(title, cat, tags, author, days, steam, kitsu, imgq, videos, body.toString().trim()));
                    }
                    title = null; cat = null; author = "testuser"; steam = null; kitsu = null; imgq = null; videos = null;
                    days = 7; tags = List.of(); body = new StringBuilder(); inBody = false;
                    continue;
                }
                if (!inBody) {
                    if (line.startsWith("TITLE:")) title = line.substring(6).trim();
                    else if (line.startsWith("CAT:")) cat = line.substring(4).trim();
                    else if (line.startsWith("TAGS:")) tags = Arrays.stream(line.substring(5).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                    else if (line.startsWith("AUTHOR:")) author = line.substring(7).trim();
                    else if (line.startsWith("DAYS:")) days = Integer.parseInt(line.substring(5).trim());
                    else if (line.startsWith("STEAM:")) steam = line.substring(6).trim();
                    else if (line.startsWith("KITSU:")) kitsu = line.substring(6).trim();
                    else if (line.startsWith("IMGQ:")) imgq = line.substring(5).trim();
                    else if (line.startsWith("VIDEO:")) {
                        List<String> vs = Arrays.stream(line.substring(6).split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList();
                        videos = vs.isEmpty() ? null : toJson(vs);
                    }
                    else if (line.trim().equals("BODY:")) inBody = true;
                } else {
                    body.append(line).append('\n');
                }
            }
            if (title != null) {
                posts.add(new Post(title, cat, tags, author, days, steam, kitsu, imgq, videos, body.toString().trim()));
            }
        }
        return posts;
    }

    /* ── 图片 ─────────────────────────────────────────────────── */

    static String resolveImage(Post p) {
        String asset = null;
        if ("GAME".equals(p.cat) && p.steam != null && !p.steam.isBlank()) {
            int idx = ASSET_USE.merge(p.steam(), 1, Integer::sum) - 1;
            asset = GAME_ASSETS[Math.min(idx, GAME_ASSETS.length - 1)];
        }
        String hash = shortHash(asset == null ? p.title() : p.title() + "|" + asset);
        Path jpg = IMG_DIR.resolve("seed-" + hash + ".jpg");
        Path png = IMG_DIR.resolve("seed-" + hash + ".png");
        try {
            if (Files.exists(jpg) && Files.size(jpg) > 30_000) return "/picture/" + jpg.getFileName();
            if (Files.exists(png) && Files.size(png) > 30_000) return "/picture/" + png.getFileName();
        } catch (IOException ignored) { }

        byte[] bytes = null;
        if (asset != null) {
            bytes = http("https://cdn.cloudflare.steamstatic.com/steam/apps/" + p.steam() + "/" + asset, 0);
            if (!valid(bytes, 600, 0)) {
                bytes = http("https://cdn.cloudflare.steamstatic.com/steam/apps/" + p.steam() + "/capsule_616x353.jpg", 0);
            }
        }
        if (!valid(bytes, 600, 0) && "ANIME".equals(p.cat) && p.kitsu != null && !p.kitsu.isBlank()) {
            try { Thread.sleep(350); } catch (InterruptedException ignored) { }
            bytes = kitsuCover(p.kitsu);
        }
        // Kitsu 缺封面时用豆瓣海报（竖图会合成为横图），比通用图搜可靠
        if (!valid(bytes, 600, 0) && "ANIME".equals(p.cat)) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) { }
            bytes = doubanPoster(p.title());
        }
        if (!valid(bytes, 600, 0)) {
            String q = p.imgq() != null && !p.imgq().isBlank() ? p.imgq()
                    : ("GAME".equals(p.cat)
                        ? p.title().replaceAll("[《》：:，,！!？?]", " ") + " 游戏截图"
                        : p.title().replaceAll("[《》：:，,！!？?]", " ") + " 动漫 壁纸");
            bytes = bingImage(q);
        }
        if (!valid(bytes, 600, 0)) {
            imgFail++;
            System.out.println("  [img] FAIL " + p.title());
            return null;
        }
        String ext = isPng(bytes) ? ".png" : ".jpg";
        Path target = IMG_DIR.resolve("seed-" + hash + ext);
        try {
            Files.write(target, bytes);
            imgOk++;
            return "/picture/" + target.getFileName();
        } catch (IOException e) {
            imgFail++;
            System.out.println("  [img] write failed " + e.getMessage());
            return null;
        }
    }

    static byte[] kitsuCover(String query) {
        try {
            String url = "https://kitsu.io/api/edge/anime?page%5Blimit%5D=1&filter%5Btext%5D=" + enc(query);
            String json = text(url, "https://kitsu.io/");
            Matcher m = Pattern.compile("\"coverImage\":\\{[^}]*?\"original\":\"(https:[^\"]+)\"").matcher(json);
            if (m.find()) {
                byte[] b = http(m.group(1), 0);
                if (valid(b, 700, 0)) return b;
            }
            Matcher p = Pattern.compile("\"posterImage\":\\{[^}]*?\"original\":\"(https:[^\"]+)\"").matcher(json);
            if (p.find()) {
                byte[] b = http(p.group(1), 0);
                if (b != null && b.length > 20_000) {
                    byte[] composed = composeLandscape(b);
                    if (composed != null) return composed;
                }
            }
        } catch (Exception e) {
            System.out.println("  [img] kitsu error: " + e.getMessage());
        }
        return null;
    }

    /** 豆瓣海报兜底：取中文标题搜索，拿到海报后合成 16:9 横图。 */
    static byte[] doubanPoster(String title) {
        try {
            String q = title;
            Matcher inside = Pattern.compile("《([^》]+)》").matcher(title);
            if (inside.find()) {
                q = inside.group(1);
            } else {
                q = title.split("[：:（(]")[0];
            }
            String json = text("https://movie.douban.com/j/subject_suggest?q=" + enc(q), "https://www.douban.com/");
            Matcher m = Pattern.compile("\"img\":\"(https:[^\"]+)\"").matcher(json);
            while (m.find()) {
                String url = m.group(1).replace("/s_ratio_poster/", "/l/");
                byte[] b = http(url, 0);
                if (b != null && b.length > 20_000) {
                    byte[] composed = composeLandscape(b);
                    if (composed != null) return composed;
                }
            }
        } catch (Exception e) {
            System.out.println("  [img] douban error: " + e.getMessage());
        }
        return null;
    }

    /** 把竖版海报合成 1280x720：模糊放大做背景 + 居中海报 + 暗角。 */
    static byte[] composeLandscape(byte[] srcBytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(srcBytes));
            if (src == null) return null;
            int W = 1280, H = 720;
            BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 背景：等比放大铺满，再缩小放大得到模糊感
            double cover = Math.max((double) W / src.getWidth(), (double) H / src.getHeight());
            int bw = (int) (src.getWidth() * cover) + 2, bh = (int) (src.getHeight() * cover) + 2;
            BufferedImage bg = new BufferedImage(Math.max(bw, W), Math.max(bh, H), BufferedImage.TYPE_INT_RGB);
            Graphics2D bgG = bg.createGraphics();
            bgG.drawImage(src, (bg.getWidth() - bw) / 2, (bg.getHeight() - bh) / 2, bw, bh, null);
            bgG.dispose();
            BufferedImage small = new BufferedImage(48, 27, BufferedImage.TYPE_INT_RGB);
            Graphics2D sg = small.createGraphics();
            sg.drawImage(bg, 0, 0, 48, 27, null);
            sg.dispose();
            g.drawImage(small, 0, 0, W, H, null);

            // 暗色蒙层
            g.setColor(new Color(14, 12, 17, 150));
            g.fillRect(0, 0, W, H);

            // 前景：海报按高度适配居中
            double fit = (double) (H - 64) / src.getHeight();
            int fw = (int) (src.getWidth() * fit), fh = (int) (src.getHeight() * fit);
            int x = (W - fw) / 2, y = (H - fh) / 2;
            g.drawImage(src, x, y, fw, fh, null);
            g.setColor(new Color(255, 255, 255, 46));
            g.drawRect(x - 1, y - 1, fw + 1, fh + 1);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            System.out.println("  [img] compose error: " + e.getMessage());
            return null;
        }
    }

    static byte[] bingImage(String query) {
        try {
            String url = "https://www.bing.com/images/search?q=" + enc(query)
                    + "&form=HDRSC2&first=1&qft=+filterui:imagesize-large+filterui:aspect-wide";
            String html = text(url, "https://www.bing.com/");
            Matcher m = Pattern.compile("murl&quot;:&quot;(https:[^&]+)&quot;").matcher(html);
            List<String> urls = new ArrayList<>();
            while (m.find() && urls.size() < 10) urls.add(m.group(1));
            for (String u : urls) {
                byte[] b = http(u, 0);
                if (valid(b, 900, 0)) return b;
            }
        } catch (Exception e) {
            System.out.println("  [img] bing error: " + e.getMessage());
        }
        return null;
    }

    static boolean valid(byte[] b, int minWidth, int minHeight) {
        if (b == null || b.length < 30_000) return false;
        if (!(isJpeg(b) || isPng(b))) return false;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));
            if (img == null) return false;
            if (img.getWidth() < minWidth) return false;
            if (minHeight > 0 && img.getHeight() < minHeight) return false;
            double ratio = (double) img.getWidth() / img.getHeight();
            return ratio >= 0.85 && ratio <= 3.6;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
    }

    static boolean isPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    static byte[] http(String url, int attempt) {
        if (attempt > 1) return null;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(12_000);
            c.setReadTimeout(30_000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "image/*,*/*");
            if (url.contains("douban")) c.setRequestProperty("Referer", "https://www.douban.com/");
            if (url.contains("bing.com") || url.contains("zhimg")) c.setRequestProperty("Referer", "https://www.bing.com/");
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) { }
            return http(url, attempt + 1);
        }
    }

    static String text(String url, String referer) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(12_000);
                c.setReadTimeout(25_000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", UA);
                c.setRequestProperty("Referer", referer);
                c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
                if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
                try (InputStream in = c.getInputStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                last = e;
                try { Thread.sleep(800L * (attempt + 1)); } catch (InterruptedException ignored) { }
            }
        }
        throw last;
    }

    /* ── 入库 ─────────────────────────────────────────────────── */

    /** 老库没有 videos 列时补上，方便单独跑种子脚本。 */
    static void ensureVideosColumn(Connection conn) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, "posts", "videos")) {
            if (!rs.next()) {
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE posts ADD COLUMN videos TEXT DEFAULT NULL COMMENT '视频链接 JSON 数组'");
                }
                System.out.println("[seed] added posts.videos");
            }
        }
    }

    record User(long id, String username, String nickname) { }

    static Map<String, User> loadUsers(Connection conn) throws SQLException {
        Map<String, User> map = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, username, nickname FROM users")) {
            while (rs.next()) {
                map.put(rs.getString("username"),
                        new User(rs.getLong("id"), rs.getString("username"), rs.getString("nickname")));
            }
        }
        return map;
    }

    static long upsert(Connection conn, Post p, String imgUrl, Map<String, User> users) throws Exception {
        User author = users.getOrDefault(p.author, users.values().iterator().next());
        String images = imgUrl == null ? null : "[\"" + imgUrl + "\"]";
        String tagsJson = p.tags().isEmpty() ? null : toJson(p.tags());
        String authorName = author.nickname() == null || author.nickname().isBlank()
                ? author.username() : author.nickname();

        Long id = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM posts WHERE title = ?")) {
            ps.setString(1, p.title());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) id = rs.getLong(1);
            }
        }

        LocalDateTime created = LocalDateTime.now()
                .minusDays(p.days())
                .withHour(9 + Math.floorMod(p.title().hashCode(), 12))
                .withMinute(Math.floorMod(p.title().hashCode(), 60))
                .withSecond(0).withNano(0);
        boolean essence = Math.floorMod(p.title().hashCode(), 5) == 0;
        boolean top = p.title().contains("指南") && p.days() <= 2;

        if (id == null) {
            String sql = "INSERT INTO posts(title,content,category,images,links,videos,user_id,author_name,"
                    + "like_count,collect_count,comment_count,is_top,is_essence,tags,created_at,updated_at) "
                    + "VALUES(?,?,?,?,NULL,?,?,?,0,0,0,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.title());
                ps.setString(2, p.body());
                ps.setString(3, p.cat());
                ps.setString(4, images);
                ps.setString(5, p.videos());
                ps.setLong(6, author.id());
                ps.setString(7, authorName);
                ps.setBoolean(8, top);
                ps.setBoolean(9, essence);
                ps.setString(10, tagsJson);
                ps.setTimestamp(11, Timestamp.valueOf(created));
                ps.setTimestamp(12, Timestamp.valueOf(created.plusHours(2)));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) id = keys.getLong(1);
                }
            }
            inserted++;
        } else {
            String sql = "UPDATE posts SET content=?, category=?, images=?, videos=?, user_id=?, author_name=?, "
                    + "is_top=?, is_essence=?, tags=?, created_at=?, updated_at=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.body());
                ps.setString(2, p.cat());
                ps.setString(3, images);
                ps.setString(4, p.videos());
                ps.setLong(5, author.id());
                ps.setString(6, authorName);
                ps.setBoolean(7, top);
                ps.setBoolean(8, essence);
                ps.setString(9, tagsJson);
                ps.setTimestamp(10, Timestamp.valueOf(created));
                ps.setTimestamp(11, Timestamp.valueOf(created.plusHours(2)));
                ps.setLong(12, id);
                ps.executeUpdate();
            }
            updated++;
        }
        System.out.println("  [post] " + (id == null ? "?" : id) + " " + p.title()
                + (imgUrl == null ? "  (no image)" : ""));
        return id == null ? -1 : id;
    }

    static void rebuildTags(Connection conn) throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT tags FROM posts WHERE tags IS NOT NULL")) {
            while (rs.next()) {
                for (String t : parseJsonArray(rs.getString(1))) {
                    counts.merge(t, 1, Integer::sum);
                }
            }
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM tags");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tags(name, post_count, created_at) VALUES(?,?,NOW())")) {
            for (var e : counts.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setInt(2, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        System.out.println("[seed] tags rebuilt: " + counts.size());
    }

    static void seedSocial(Connection conn, List<Long> ids, Map<String, User> users) throws Exception {
        if (ids.isEmpty()) return;
        String in = String.join(",", Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM likes WHERE post_id IN (" + in + ")")) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM collects WHERE post_id IN (" + in + ")")) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comments WHERE post_id IN (" + in + ")")) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            ps.executeUpdate();
        }

        List<User> pool = new ArrayList<>(users.values());
        pool.sort(Comparator.comparingLong(User::id));
        Random rnd = new Random(20261025L);
        int likes = 0, collects = 0, comments = 0;

        try (PreparedStatement like = conn.prepareStatement(
                "INSERT INTO likes(user_id, post_id, created_at) VALUES(?,?,?)");
             PreparedStatement collect = conn.prepareStatement(
                     "INSERT INTO collects(user_id, post_id, created_at) VALUES(?,?,?)");
             PreparedStatement cmt = conn.prepareStatement(
                     "INSERT INTO comments(content, post_id, user_id, user_name, parent_id, created_at) VALUES(?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            for (Long id : ids) {
                Timestamp base = postCreatedAt(conn, id);
                Set<Long> liked = new HashSet<>();
                for (User u : pool) {
                    if (u.id() == authorId(conn, id)) continue;
                    if (Math.floorMod(id * 31 + u.id() * 17, 3) != 0) {
                        like.setLong(1, u.id());
                        like.setLong(2, id);
                        like.setTimestamp(3, plusHours(base, 1 + (int) (u.id() % 20)));
                        like.addBatch();
                        liked.add(u.id());
                        likes++;
                    }
                    if (Math.floorMod(id * 13 + u.id() * 7, 2) == 0) {
                        collect.setLong(1, u.id());
                        collect.setLong(2, id);
                        collect.setTimestamp(3, plusHours(base, 2 + (int) (u.id() % 18)));
                        collect.addBatch();
                        collects++;
                    }
                }
                like.executeBatch();
                collect.executeBatch();

                int n = 2 + Math.floorMod(id, 4);
                List<Long> rootIds = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    User cu = pool.get((int) Math.floorMod(id + i * 7, pool.size()));
                    String text = COMMENT_POOL.get((int) Math.floorMod(id * 5 + i * 3, COMMENT_POOL.size()));
                    cmt.setString(1, text);
                    cmt.setLong(2, id);
                    cmt.setLong(3, cu.id());
                    cmt.setString(4, cu.nickname());
                    cmt.setNull(5, Types.BIGINT);
                    cmt.setTimestamp(6, plusHours(base, 3 + i * 5));
                    cmt.executeUpdate();
                    try (ResultSet keys = cmt.getGeneratedKeys()) {
                        if (keys.next()) rootIds.add(keys.getLong(1));
                    }
                    comments++;
                }
                if (!rootIds.isEmpty()) {
                    for (int r = 0; r < 2; r++) {
                        User ru = pool.get((int) Math.floorMod(id + r * 3 + 1, pool.size()));
                        String text = REPLY_POOL.get((int) Math.floorMod(id + r * 11, REPLY_POOL.size()));
                        cmt.setString(1, text);
                        cmt.setLong(2, id);
                        cmt.setLong(3, ru.id());
                        cmt.setString(4, ru.nickname());
                        cmt.setLong(5, rootIds.get(0));
                        cmt.setTimestamp(6, plusHours(base, 24 + r * 6));
                        cmt.executeUpdate();
                        comments++;
                    }
                }
            }
        }
        System.out.println("[seed] likes=" + likes + ", collects=" + collects + ", comments=" + comments);
    }

    static void recomputeCounts(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE posts p SET "
                    + "like_count=(SELECT COUNT(*) FROM likes l WHERE l.post_id=p.id), "
                    + "collect_count=(SELECT COUNT(*) FROM collects c WHERE c.post_id=p.id), "
                    + "comment_count=(SELECT COUNT(*) FROM comments cm WHERE cm.post_id=p.id)");
        }
    }

    static Timestamp postCreatedAt(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT created_at FROM posts WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getTimestamp(1);
            }
        }
        return new Timestamp(System.currentTimeMillis());
    }

    static long authorId(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM posts WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    static Timestamp plusHours(Timestamp t, int h) {
        return new Timestamp(t.getTime() + h * 3600_000L);
    }

    /* ── 小工具 ───────────────────────────────────────────────── */

    static List<String> parseJsonArray(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        Matcher m = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        while (m.find()) out.add(m.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
        return out;
    }

    static String toJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return sb.append(']').toString();
    }

    static String shortHash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static final List<String> COMMENT_POOL = List.of(
            "刚通关，看到这篇又想重开二周目了。",
            "这个思路我居然没试过，回头去试试。",
            "写得太细了，先收藏，慢慢研究。",
            "楼主是不是漏了中后期的一个机制？我打的时候感觉更吃走位。",
            "看完感觉我又可以了，今晚就上线。",
            "同感，尤其是节奏这块，慢下来反而更有味道。",
            "这个点我踩过坑，补充一句：别急着堆数值，先把手感练熟。",
            "已经安利给朋友了，他说今晚就来入坑。",
            "画面和音乐确实顶，剧情后半段我直接沉默。",
            "有没有更低门槛的打法？手残党表示压力很大。",
            "我刚好卡在这里，感谢楼主救命。",
            "这作最打动我的是细节，不是大场面。",
            "不同周目体验差好多，建议至少二刷。",
            "楼主文笔真好，看着像在听人聊天。",
            "先码住，周末慢慢看，顺便把游戏捡起来。",
            "这个系列我全通了，这篇总结得很到位。",
            "补充一个冷知识：这个设计其实在初代就有雏形。",
            "看完立刻去翻原声带，太好听了。"
    );

    static final List<String> REPLY_POOL = List.of(
            "同感，尤其是最后那段。",
            "收到，我按这个思路再试一次。",
            "哈哈我也是手残，慢慢磨吧。",
            "这个补充很有用，谢谢！",
            "二周目确实值得，细节多到爆炸。",
            "原声带已经循环一整天了。",
            "楼主更新了记得踢我一下。",
            "先别剧透，我还没打到那里。"
    );
}
