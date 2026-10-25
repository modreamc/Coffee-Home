package com.forum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.dto.VideoMeta;
import com.forum.util.VideoLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视频元信息抓取：
 * <ul>
 *   <li>B站：调用公开的 view 接口拿标题 / 封面 / 时长 / UP 主，并把封面下载到本地 picture 目录；</li>
 *   <li>b23.tv：服务端跟随一次重定向解析出真实 BV 号；</li>
 *   <li>YouTube：oEmbed（网络不可达时自动降级为只有链接）；</li>
 *   <li>腾讯 / 优酷 / 抖音：没有稳定的公开元信息接口，只做平台识别。</li>
 * </ul>
 * 只访问固定的平台接口，不接受任意 URL，避免 SSRF。
 */
@Service
public class VideoMetaService {

    private static final Logger log = LoggerFactory.getLogger(VideoMetaService.class);
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final long TTL_MS = 6 * 3600_000L;
    private static final long FAIL_TTL_MS = 10 * 60_000L;
    private static final int CACHE_LIMIT = 500;
    private static final Set<String> NO_EMBED = Set.of("douyin", "ixigua");

    private record CacheEntry(VideoMeta meta, long expireAt) {
    }

    private final ObjectMapper objectMapper;
    private final ImageService imageService;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public VideoMetaService(ObjectMapper objectMapper, ImageService imageService) {
        this.objectMapper = objectMapper;
        this.imageService = imageService;
    }

    /** 批量补全（发帖/改帖时调用），最多 3 条，忽略超限。 */
    public List<VideoMeta> enrich(List<String> urls) {
        List<VideoMeta> result = new ArrayList<>();
        for (String url : VideoLinks.normalize(urls)) {
            result.add(fetch(url));
        }
        return result;
    }

    public VideoMeta fetch(String raw) {
        String url = VideoLinks.clean(raw);
        if (url == null) throw new RuntimeException("不支持的视频链接");
        CacheEntry cached = cache.get(url);
        if (cached != null && cached.expireAt() > System.currentTimeMillis()) {
            return cached.meta();
        }
        VideoMeta meta = doFetch(url);
        long ttl = meta.title() == null ? FAIL_TTL_MS : TTL_MS;
        cache.put(url, new CacheEntry(meta, System.currentTimeMillis() + ttl));
        if (cache.size() > CACHE_LIMIT) {
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(e -> e.getValue().expireAt() < now);
        }
        return meta;
    }

    private VideoMeta doFetch(String url) {
        VideoLinks.Ref ref = VideoLinks.parse(url);
        if (ref == null) return fallback(url, "unknown", null);

        if ("b23".equals(ref.provider())) {
            String resolved = resolveShortLink(url);
            if (resolved != null) {
                VideoLinks.Ref real = VideoLinks.parse(resolved);
                if (real != null) {
                    url = resolved;
                    ref = real;
                }
            }
        }

        if ("bilibili".equals(ref.provider())) return bilibili(url, ref.videoId());
        if ("youtube".equals(ref.provider())) return youtube(url, ref.videoId());
        return fallback(url, ref.provider(), ref.videoId());
    }

    private VideoMeta bilibili(String url, String id) {
        try {
            String api = id != null && id.startsWith("av")
                    ? "https://api.bilibili.com/x/web-interface/view?aid=" + id.substring(2)
                    : "https://api.bilibili.com/x/web-interface/view?bvid=" + id;
            JsonNode root = objectMapper.readTree(text(api, "https://www.bilibili.com/", 6000));
            if (root.path("code").asInt(-1) != 0) {
                log.info("bilibili meta failed: code={} url={}", root.path("code").asInt(), url);
                return fallback(url, "bilibili", id);
            }
            JsonNode data = root.path("data");
            String bvid = data.path("bvid").asText(id);
            String title = data.path("title").asText(null);
            String owner = data.path("owner").path("name").asText(null);
            int duration = data.path("duration").asInt(0);
            String cover = downloadCover(data.path("pic").asText(""), bvid);
            return new VideoMeta(url, "bilibili", bvid, title, cover, duration, owner, true);
        } catch (Exception e) {
            log.warn("bilibili meta error: {}", e.getMessage());
            return fallback(url, "bilibili", id);
        }
    }

    private String downloadCover(String picUrl, String bvid) {
        if (picUrl == null || picUrl.isBlank()) return null;
        String httpsUrl = picUrl.startsWith("http://") ? "https://" + picUrl.substring(7) : picUrl;
        if (!httpsUrl.contains("hdslb.com") && !httpsUrl.contains("bilibili.com")) return null;
        try {
            byte[] bytes = http(httpsUrl, "https://www.bilibili.com/", 8000);
            return imageService.saveRemoteImage(bytes, "video-" + bvid);
        } catch (Exception e) {
            log.warn("bilibili cover download failed: {}", e.getMessage());
            return null;
        }
    }

    private VideoMeta youtube(String url, String videoId) {
        try {
            String api = "https://www.youtube.com/oembed?format=json&url="
                    + java.net.URLEncoder.encode("https://www.youtube.com/watch?v=" + videoId, java.nio.charset.StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(text(api, "https://www.youtube.com/", 4000));
            String title = node.path("title").asText(null);
            String author = node.path("author_name").asText(null);
            String cover = node.path("thumbnail_url").asText(null);
            return new VideoMeta(url, "youtube", videoId, title, cover, null, author, true);
        } catch (Exception e) {
            log.info("youtube oembed unavailable: {}", e.getMessage());
            return fallback(url, "youtube", videoId);
        }
    }

    private VideoMeta fallback(String url, String provider, String id) {
        boolean embeddable = provider != null && !NO_EMBED.contains(provider) && !"unknown".equals(provider);
        return new VideoMeta(url, provider, id, null, null, null, null, embeddable);
    }

    /** b23.tv 短链：跟随重定向，只接受落在 bilibili.com 的结果。 */
    private String resolveShortLink(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", "https://www.bilibili.com/");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) return null;
            URL finalUrl = conn.getURL();
            String finalString = finalUrl.toString();
            return finalString.contains("bilibili.com") ? finalString : null;
        } catch (Exception e) {
            log.warn("resolve b23 failed: {}", e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String text(String url, String referer, int timeoutMs) throws Exception {
        byte[] bytes = http(url, referer, timeoutMs);
        return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] http(String url, String referer, int timeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", referer);
            conn.setRequestProperty("Accept", "*/*");
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                return in.readAllBytes();
            }
        } finally {
            conn.disconnect();
        }
    }
}
