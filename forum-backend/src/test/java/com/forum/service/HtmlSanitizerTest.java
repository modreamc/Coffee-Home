package com.forum.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class HtmlSanitizerTest {
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void removesScriptAndEventHandlers() {
        String dirty = "<p onclick=\"alert(1)\">hi</p><script>alert(2)</script>"
                + "<img src=\"/picture/a.jpg\" onerror=\"alert(3)\">";
        String clean = sanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("<script").doesNotContain("onerror").doesNotContain("onclick");
        assertThat(clean).contains("hi");
    }

    @Test
    void keepsSafeFormattingAndRelativeImages() {
        String clean = sanitizer.sanitize("<p>正文 <b>加粗</b></p><img src=\"/picture/a.jpg\" alt=\"x\">");
        assertThat(clean).contains("<b>加粗</b>");
        assertThat(clean).contains("src=\"/picture/a.jpg\"");
    }

    @Test
    void blocksJavascriptProtocol() {
        String clean = sanitizer.sanitize("<a href=\"javascript:alert(1)\">click</a>");
        assertThat(clean).doesNotContain("javascript:");
    }

    @Test
    void keepsExternalHttpsImage() {
        String clean = sanitizer.sanitize("<img src=\"https://cdn.example.com/a.png\">");
        assertThat(clean).contains("https://cdn.example.com/a.png");
    }

    @Test
    void plainTextStripsAllTags() {
        assertThat(sanitizer.toPlainText("<b>标题</b><script>x</script>")).isEqualTo("标题");
    }
}
