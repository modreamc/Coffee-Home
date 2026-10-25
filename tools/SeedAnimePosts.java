import java.sql.*;
import java.util.*;

/**
 * 动漫板块种子数据填充（含错开的发布时间）
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." SeedAnimePosts.java
 */
public class SeedAnimePosts {
    static class Seed {
        String title; String content; String author; long userId; String tags; int daysAgo;
        Seed(String t, String c, String a, long u, String tg, int d) {
            title = t; content = c; author = a; userId = u; tags = tg; daysAgo = d;
        }
    }
    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        List<Seed> seeds = new ArrayList<>();
        seeds.add(new Seed("《葬送的芙莉莲》完结感想：温柔得让人眼眶发热",
                "<p>追完芙莉莲真的怅然若失。这部番把\"时间\"这个概念拍得太动人了——精灵漫长的寿命里，人类的百年只是一瞬。</p><p><b>最打动我的</b>是勇者辛美尔那句\"下次见面再说\"，以及芙莉莲后来一点一点理解人类情感的旅程。</p><p>音乐、作画、叙事节奏全部在线，年度最佳毫不夸张。没看的快去补！</p>",
                "crk", 3, "[\"安利\",\"芙莉莲\"]", 9));
        seeds.add(new Seed("《进击的巨人》终章感想：十年追番，终于画上句号",
                "<p>从第一季追到完结，看着艾伦从少年变成那样，心情真的很复杂。</p><p>谏山创的结局争议很大，但对我而言，这部作品对\"自由\"的探讨已经足够深刻。马莱篇的文戏是整部作品的高光，立体机动装置的战斗场面更是动画工业的巅峰。</p><p>谢谢你，谏山创。谢谢你，WIT和MAPPA。</p>",
                "管理员", 1, "[\"漫评\",\"进击的巨人\"]", 8));
        seeds.add(new Seed("《孤独摇滚》：为什么说它是音乐番天花板",
                "<p>本以为只是萌系日常，结果被波奇酱的社恐真实感击中。这部的核心其实是\"想被看见\"的青春焦虑。</p><p><b>作画演出</b>：live 场景的运镜和演出水准是电影级的，最后一集甚至玩起了摇滚动画里的意识流。</p><p><b>音乐</b>：结束乐队的曲子每一首都好听，编曲有心思，贝斯和鼓的音色很讲究。</p><p>不夸张地说，它让很多人重新拿起了乐器。</p>",
                "testuser", 2, "[\"安利\",\"孤独摇滚\"]", 7));
        seeds.add(new Seed("求推荐：类似《夏日重现》的悬疑轮回系作品",
                "<p>刚补完夏日重现，被它的悬疑节奏和轮回设定圈粉了，求类似作品！</p><p>已经看过：《命运石之门》《寒蝉鸣泣之时》《RE:0》《重启咲良田》。</p><p>最好是叙事紧凑、逻辑能自洽、结局不烂尾的，电影也行。评论区拜托了！</p>",
                "123", 4, "[\"求助\",\"悬疑\"]", 6));
        seeds.add(new Seed("《迷宫饭》和《芙莉莲》：奇幻日常的两种打开方式",
                "<p>今年两部奇幻日常系大作，风格截然不同却都好看：</p><p><b>《迷宫饭》</b>：美食+地下城，把魔物当食材的脑洞让人拍案叫绝，莱欧斯一队人的化学反应特别有趣，而且后期世界观展开完全超出日常番的预期。</p><p><b>《芙莉莲》</b>：旅途+回忆，用散文式的节奏讲时间与羁绊，安静但后劲十足。</p><p>两部都强推，先看哪部都行，不会失望。</p>",
                "crk", 3, "[\"漫评\",\"迷宫饭\"]", 5));
        seeds.add(new Seed("老番补番指南：这三部入门神作先看哪个？",
                "<p>给刚入坑的朋友的补番顺序建议，都是经得起时间考验的作品：</p><p><b>《钢之炼金术师 FA》</b>：叙事工整、节奏完美、结局圆满，零差评神作，最适合入门。</p><p><b>《死亡笔记》</b>：智斗天花板，前 25 集一气呵成，看完会大呼过瘾。</p><p><b>《命运石之门》</b>：慢热但第 12 集开始封神，看完记得回头二刷。</p><p>顺序：钢炼 → 死亡笔记 → 石头门，保证体验最佳。</p>",
                "管理员", 1, "[\"攻略\",\"补番\"]", 4));
        seeds.add(new Seed("《我推的孩子》第二季观后感：舞台上的挣扎与光芒",
                "<p>第二季把重点放在舞台剧篇，赤坂明这次在\"表演\"上下了很大功夫。</p><p>对\"星野爱\"的补完、对阿库亚与露比各自执念的展开，都让角色更立体了。动画对舞台演出的表现力依然在线，特别是最后几集的临场感。</p><p>唯一遗憾是剧情推进有点慢，但为了最后的爆点，值得等。</p>",
                "testuser", 2, "[\"漫评\",\"我推的孩子\"]", 3));
        seeds.add(new Seed("2026 春季新番前瞻：这季值得追的都在这里",
                "<p>按题材给大家整理了这季值得关注的新番：</p><p><b>热血战斗</b>：期待值拉满的续作们，作画监督阵容豪华，值得首集观望。</p><p><b>治愈日常</b>：本季最大黑马候选，适合下班后放松看。</p><p><b>原创番</b>：永远的神鬼难测，但原创的惊喜也最大，建议每部都给三集机会。</p><p>具体名字先不剧透，等开播后我再来详细写推荐～</p>",
                "crk", 3, "[\"攻略\",\"新番\"]", 2));
        seeds.add(new Seed("《咒术回战》涩谷事变篇：这个篇章值得封神",
                "<p>涩谷事变篇是我认为咒术回战到目前为止最好的篇章。</p><p><b>战斗设计</b>：五条悟的领域展开、宿傩的疯狂、虎杖的挣扎，多线并行剪辑得干净利落。</p><p><b>情绪张力</b>：从\"最强\"的坠落开始，整个篇章弥漫着绝望与牺牲的气息，看完会沉默很久。</p><p>MAPPA 的制作依然稳定，打斗作画拉满。没看到这里的快冲。</p>",
                "管理员", 1, "[\"安利\",\"咒术回战\"]", 1));
        seeds.add(new Seed("最近在追什么番？来互相安利一下",
                "<p>新季度开播两周了，大家在追什么？我先来：</p><p>目前在追芙莉莲二刷 + 迷宫饭，还在补葬送系列的漫画。上半年补完了《兽娘动物园》和《奇蛋物语》，后者结局争议挺大但美术是真好。</p><p>评论区说说你最近在看的，好的话我也去补！</p>",
                "123", 4, "[\"闲聊\"]", 0));

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Set<String> allTags = new LinkedHashSet<>();
            String insertSql = "INSERT INTO posts (title, content, category, images, links, user_id, author_name, like_count, collect_count, comment_count, is_top, is_essence, tags, created_at, updated_at) " +
                    "VALUES (?,?,?,NULL,NULL,?,?,0,0,0,0,0,?, DATE_SUB(NOW(), INTERVAL " + 0 + " DAY), DATE_SUB(NOW(), INTERVAL " + 0 + " DAY))";
            int count = 0;
            for (Seed s : seeds) {
                String sql = "INSERT INTO posts (title, content, category, images, links, user_id, author_name, like_count, collect_count, comment_count, is_top, is_essence, tags, created_at, updated_at) " +
                        "VALUES (?,?,?,NULL,NULL,?,?,0,0,0,0,0,?, DATE_SUB(NOW(), INTERVAL " + s.daysAgo + " DAY), DATE_SUB(NOW(), INTERVAL " + s.daysAgo + " DAY))";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, s.title);
                    ps.setString(2, s.content);
                    ps.setString(3, "ANIME");
                    ps.setLong(4, s.userId);
                    ps.setString(5, s.author);
                    ps.setString(6, s.tags);
                    ps.executeUpdate();
                    count++;
                }
                if (s.tags != null) {
                    String[] arr = s.tags.replaceAll("[\\[\\]\"]", "").split(",");
                    Collections.addAll(allTags, arr);
                }
            }
            int tagCount = 0;
            try (PreparedStatement pt = conn.prepareStatement(
                    "INSERT INTO tags (name, post_count, created_at) VALUES (?,1,NOW()) " +
                    "ON DUPLICATE KEY UPDATE post_count = post_count + 1")) {
                for (String t : allTags) {
                    pt.setString(1, t.trim());
                    pt.addBatch();
                    tagCount++;
                }
                pt.executeBatch();
            }
            System.out.println("OK inserted posts: " + count);
            System.out.println("OK tags maintained: " + tagCount);
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM posts WHERE category='ANIME'")) {
                if (rs.next()) System.out.println("ANIME total: " + rs.getLong(1));
            }
        }
    }
}
