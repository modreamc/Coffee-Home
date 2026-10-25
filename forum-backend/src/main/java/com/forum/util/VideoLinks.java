package com.forum.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频链接白名单。
 * <p>
 * 不允许用户直接往正文里塞 iframe（XSS / 任意嵌入风险），
 * 只接受受支持平台的链接，前端再把链接解析成官方播放器地址。
 */
public final class VideoLinks {

    public static final int MAX = 3;

    /** 支持 iframe 或可直接跳转的平台主域名。 */
    private static final Set<String> DOMAINS = Set.of(
            "bilibili.com",
            "b23.tv",
            "youtube.com",
            "youtu.be",
            "v.qq.com",
            "youku.com",
            "douyin.com",
            "ixigua.com"
    );

    private VideoLinks() {
    }

    /** 清洗 + 去重 + 限流，返回可直接入库的链接列表。 */
    public static List<String> normalize(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        LinkedHashSet<String> accepted = new LinkedHashSet<>();
        for (String item : raw) {
            String cleaned = clean(item);
            if (cleaned != null) accepted.add(cleaned);
            if (accepted.size() >= MAX) break;
        }
        return new ArrayList<>(accepted);
    }

    /** 单个链接校验：只允许 http(s) + 白名单域名。 */
    public static String clean(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 500) return null;
        if (value.startsWith("//")) value = "https:" + value;
        if (!value.matches("(?i)^https?://.*")) value = "https://" + value;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (scheme == null) return null;
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
            String host = uri.getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            String finalHost = host;
            boolean allowed = DOMAINS.stream()
                    .anyMatch(d -> finalHost.equals(d) || finalHost.endsWith("." + d));
            if (!allowed) return null;
            return uri.normalize().toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析出的平台与视频 ID。 */
    public record Ref(String provider, String videoId) {
    }

    /**
     * 从链接里解析平台和 videoId（不做网络请求）。
     * b23.tv 短链会返回 provider=b23，由上层做一次重定向解析。
     */
    public static Ref parse(String raw) {
        String value = clean(raw);
        if (value == null) return null;
        URI uri;
        try {
            uri = URI.create(value);
        } catch (Exception e) {
            return null;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) host = host.substring(4);
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.equals("b23.tv")) {
            String code = path.replaceFirst("^/", "");
            return code.isEmpty() ? null : new Ref("b23", code);
        }
        if (host.endsWith("bilibili.com")) {
            Matcher bv = BILIBILI_BV.matcher(path);
            if (bv.find()) return new Ref("bilibili", bv.group(1));
            Matcher av = BILIBILI_AV.matcher(path);
            if (av.find()) return new Ref("bilibili", "av" + av.group(1));
            return null;
        }
        if (host.endsWith("youtube.com") || host.equals("youtu.be")) {
            String id = null;
            if (host.equals("youtu.be")) {
                id = path.replaceFirst("^/", "").split("/")[0];
            } else {
                String q = uri.getQuery();
                if (q != null) {
                    for (String part : q.split("&")) {
                        if (part.startsWith("v=")) id = part.substring(2);
                    }
                }
                if (id == null) {
                    Matcher m = YOUTUBE_PATH.matcher(path);
                    if (m.find()) id = m.group(1);
                }
            }
            return id == null || id.isBlank() ? null : new Ref("youtube", id);
        }
        if (host.endsWith("v.qq.com")) {
            Matcher m = TENCENT.matcher(path);
            return m.find() ? new Ref("tencent", m.group(1)) : null;
        }
        if (host.endsWith("youku.com")) {
            Matcher m = YOUKU.matcher(path);
            return m.find() ? new Ref("youku", m.group(1)) : null;
        }
        if (host.endsWith("douyin.com")) {
            Matcher m = DOUYIN.matcher(path);
            return new Ref("douyin", m.find() ? m.group(1) : null);
        }
        if (host.endsWith("ixigua.com")) {
            Matcher m = IXIGUA.matcher(path);
            return new Ref("ixigua", m.find() ? m.group(1) : null);
        }
        return null;
    }

    private static final Pattern BILIBILI_BV = Pattern.compile("/video/(BV[0-9A-Za-z]{10})");
    private static final Pattern BILIBILI_AV = Pattern.compile("/video/av(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern YOUTUBE_PATH = Pattern.compile("/(?:embed|shorts)/([\\w-]{11})");
    private static final Pattern TENCENT = Pattern.compile("/x/page/(\\w+)\\.html");
    private static final Pattern YOUKU = Pattern.compile("/v_show/id_([\\w=]+)\\.html");
    private static final Pattern DOUYIN = Pattern.compile("/video/(\\d+)");
    private static final Pattern IXIGUA = Pattern.compile("/video/(\\d+)");
}
