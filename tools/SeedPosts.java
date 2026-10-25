import java.sql.*;
import java.util.*;

/**
 * 游戏板块种子数据填充
 * 用法: java -cp "mysql-connector-j-8.3.0.jar;." SeedPosts.java
 */
public class SeedPosts {
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
        seeds.add(new Seed("《塞尔达传说：王国之泪》新手开局指南：别急着去地底",
                "<p>很多新玩家一拿到游戏就直奔地底世界，结果被瘴气教育得怀疑人生。这里给刚登上海拉鲁大陆的朋友几点建议：</p><p><b>1. 先做主线到天空岛教程结束</b>，拿到滑翔伞再到处乱跑。</p><p><b>2. 前期别硬刚怪物营地</b>，看到猪猪大队绕道走，攒武器比莽更实在。</p><p><b>3. 神庙优先升精力</b>，两圈精力条能救你很多次。</p><p>祝大家玩得开心，有问题楼下问～</p>",
                "crk", 3, "[\"攻略\",\"塞尔达\"]", 6));
        seeds.add(new Seed("艾尔登法环一周目通关心得：褪色者永不屈服",
                "<p>断断续续打了两个月，总算把艾尔登法环一周目打通了。分享几个实用心得：</p><p><b>近战流</b>：优先加血量和耐力，前期拿猎犬长牙可以一路用到通关。</p><p><b>法师流</b>：陨石杖 + 岩石球开局，前中期横着走，后期记得换暗月大剑。</p><p><b>心态建议</b>：打不过就换条路，开放世界不是让你头铁的。</p><p>最后想说，这款游戏的美术和地图设计真的是艺术品，强烈推荐。</p>",
                "管理员", 1, "[\"攻略\",\"艾尔登法环\"]", 5));
        seeds.add(new Seed("星露谷物语新手向：第一年怎么过最舒服",
                "<p>星露谷第一年是最容易迷茫的，其实核心就三件事：</p><p><b>种地</b>：春季防风草、夏季蓝莓、秋季南瓜，别贪多种，浇水时间要算好。</p><p><b>钓鱼</b>：前期钓鱼来钱最快，春雨时节还能钓到鲶鱼。</p><p><b>社交</b>：每周给喜欢的 NPC 送礼，生日当天送礼效果翻倍。</p><p>第一年别急着买房子，优先升工具和背包，节奏会舒服很多。</p>",
                "crk", 3, "[\"攻略\",\"种田\"]", 4));
        seeds.add(new Seed("黑神话悟空：虎先锋打不过？试试这三招",
                "<p>虎先锋确实是前期劝退怪，给卡住的朋友几个思路：</p><p><b>1. 拉开距离等扑击</b>，它扑空后硬直很大，这时候输出。</p><p><b>2. 带足丹药</b>，回血药拉满，别省。</p><p><b>3. 实在不行去刷级</b>，等级压制永远是最稳的解法。</p><p>过了虎先锋后面其实就顺了，加油天命人！</p>",
                "testuser", 2, "[\"求助\",\"黑神话\"]", 3));
        seeds.add(new Seed("最近通了两款宝藏独立游戏，安利给大家",
                "<p>这个月通关的两款独立游戏都让我眼前一亮：</p><p><b>《哈迪斯》</b>：肉鸽+希腊神话，手感丝滑，剧情也动人，死亡次数越多剧情解锁越多，越玩越上头。</p><p><b>《空洞骑士》</b>：虽然有点难，但地图设计和氛围感无敌，沉默的骑士也有让人心碎的故事。</p><p>都是几十块的价位，性价比拉满，冲就完事了。</p>",
                "管理员", 1, "[\"安利\",\"独立游戏\"]", 2));
        seeds.add(new Seed("文明6新手最容易忽略的五个细节",
                "<p>文明6对新手不太友好，很多机制藏得很深，这里挑五个最容易被忽略的：</p><p><b>1. 区域相邻加成</b>：学院旁边放山+图书馆，收益天差地别。</p><p><b>2. 宜居度</b>：城市太多宜居度会崩，人口增长会停滞。</p><p><b>3. 政体切换</b>：解锁新政体记得及时切换，别死磕一个。</p><p><b>4. 贸易路线</b>：商路带来的金币和食物非常可观。</p><p><b>5. 胜利条件</b>：开局先想好走哪种胜利，别样样都想发展。</p>",
                "123", 4, "[\"攻略\",\"文明6\"]", 2));
        seeds.add(new Seed("博德之门3职业推荐：新手该选什么？",
                "<p>博德之门3的职业选择多到眼花缭乱，给新手朋友几个推荐：</p><p><b>战士</b>：简单粗暴，冲锋陷阵，操作门槛低，最适合第一次接触回合制CRPG的玩家。</p><p><b>吟游诗人</b>：全能型选手，能打能奶能嘴炮，对话选项也多，剧情体验好。</p><p><b>圣武士</b>：面板好看，后期爆发强，就是道德选择要慎重。</p><p>选好职业，拉上影心，出发吧！</p>",
                "crk", 3, "[\"攻略\",\"博德之门3\"]", 1));
        seeds.add(new Seed("只狼：苇名一心无伤打法心得",
                "<p>打了三天终于无伤过了苇名一心，分享下关键思路：</p><p><b>第一阶段</b>：贴身压制，它出刀就弹反，节奏稳定后非常公式化。</p><p><b>第二阶段</b>：注意它的突刺危字，用识破反击。</p><p><b>第三阶段</b>：反而简单，雷电奉还可以直接反击打大硬直。</p><p>核心就是：别贪刀，弹反优先，一心其实比弦一郎好打。</p>",
                "testuser", 2, "[\"攻略\",\"只狼\"]", 0));
        seeds.add(new Seed("游戏荒了，最近大家在玩什么？",
                "<p>最近通关了几款大作之后突然游戏荒了，单机游戏区的大佬们都在玩啥？</p><p>想找点耐玩、能沉浸进去的，最好是剧情向或者经营类的，欢迎大家评论区推荐，先谢过各位了！</p>",
                "管理员", 1, "[\"闲聊\"]", 0));
        seeds.add(new Seed("《极乐迪斯科》通关感想：它不像游戏，更像一场对话",
                "<p>花了二十多个小时打完《极乐迪斯科》，现在还没缓过来。</p><p>这游戏没有战斗，全靠对话和选择推进，但它的文字量和对人性的剖析远超大多数电影和小说。你扮演的警探脑子里有几十种声音，每种都代表一种意识形态。</p><p>如果你愿意静下心读文字，这可能是你玩过最特别的游戏之一。</p>",
                "crk", 3, "[\"安利\",\"极乐迪斯科\"]", 0));

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // 收集所有标签用于维护 tags 表
            Set<String> allTags = new LinkedHashSet<>();
            String insertSql = "INSERT INTO posts (title, content, category, images, links, user_id, author_name, like_count, collect_count, comment_count, is_top, is_essence, tags, created_at, updated_at) VALUES (?,?,?,NULL,NULL,?,?,0,0,0,0,0,?,NOW() - INTERVAL ? DAY, NOW() - INTERVAL ? DAY)";
            int count = 0;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Seed s : seeds) {
                    ps.setString(1, s.title);
                    ps.setString(2, s.content);
                    ps.setString(3, "GAME");
                    ps.setLong(4, s.userId);
                    ps.setString(5, s.author);
                    ps.setString(6, s.tags);
                    ps.setInt(7, s.daysAgo);
                    ps.setInt(8, s.daysAgo);
                    ps.addBatch();
                    if (s.tags != null) {
                        String[] arr = s.tags.replaceAll("[\\[\\]\"]", "").split(",");
                        Collections.addAll(allTags, arr);
                    }
                }
                ps.executeBatch();
                count = seeds.size();
            }
            // 维护 tags 表计数
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
            System.out.println("✅ 插入帖子: " + count + " 条");
            System.out.println("✅ 维护标签: " + tagCount + " 个");
            // 验证
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM posts WHERE category='GAME'")) {
                if (rs.next()) System.out.println("📊 GAME 板块帖子总数: " + rs.getLong(1));
            }
        }
    }
}
