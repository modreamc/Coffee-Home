package com.forum.service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
/**
 * 正文 HTML 白名单过滤。
 * 富文本需要保留排版能力，但必须干掉 script / iframe / 事件属性 / javascript: 协议，
 * 否则任何登录用户都能在帖子详情页（v-html）里执行任意 JS。
 */
@Service
public class HtmlSanitizer {
    /** 用固定基地址解析相对路径，清洗后再还原，保证 /picture/xxx.jpg 这类站内图片不被丢弃。 */
    private static final String BASE_URI = "https://sanitize.invalid/";
    private final Safelist safelist;

    public HtmlSanitizer() {
        this.safelist = Safelist.relaxed()
                .addTags("u", "s", "h1", "h2", "h3", "blockquote", "pre", "code", "hr")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addAttributes("a", "href", "title")
                .addAttributes("ol", "start")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("a", "href", "http", "https", "mailto")
                .removeTags("script", "style", "iframe", "object", "embed", "form", "input", "button", "link", "meta")
                .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");
    }

    public String sanitize(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) return "";
        Document doc = Jsoup.parseBodyFragment(rawHtml, BASE_URI);
        doc.outputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(doc.body().html(), BASE_URI, safelist);
        return cleaned.replace(BASE_URI, "/");
    }

    /** 纯文本场景（如标题、通知内容）去掉所有标签。 */
    public String toPlainText(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return Jsoup.clean(raw, Safelist.none());
    }
}
