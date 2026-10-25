package com.forum.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VideoLinksTest {

    @Test
    void acceptsSupportedPlatforms() {
        assertThat(VideoLinks.clean("https://www.bilibili.com/video/BV1GJ411x7h7")).isNotNull();
        assertThat(VideoLinks.clean("https://b23.tv/abc123")).isNotNull();
        assertThat(VideoLinks.clean("https://youtu.be/dQw4w9WgXcQ")).isNotNull();
        assertThat(VideoLinks.clean("https://v.qq.com/x/page/abc123.html")).isNotNull();
        assertThat(VideoLinks.clean("https://v.youku.com/v_show/id_XNTk.html")).isNotNull();
        assertThat(VideoLinks.clean("https://www.douyin.com/video/123456")).isNotNull();
    }

    @Test
    void rejectsUnknownOrDangerousSchemes() {
        assertThat(VideoLinks.clean("https://evil.example.com/video/1")).isNull();
        assertThat(VideoLinks.clean("javascript:alert(1)")).isNull();
        assertThat(VideoLinks.clean("file:///etc/passwd")).isNull();
        assertThat(VideoLinks.clean("")).isNull();
        assertThat(VideoLinks.clean(null)).isNull();
    }

    @Test
    void addsSchemeAndStripsWwwForHostCheck() {
        assertThat(VideoLinks.clean("www.bilibili.com/video/BV1xx411c7mD"))
                .isEqualTo("https://www.bilibili.com/video/BV1xx411c7mD");
    }

    @Test
    void deduplicatesAndCapsAtThree() {
        List<String> result = VideoLinks.normalize(List.of(
                "https://www.bilibili.com/video/BV1aa411c7mD",
                "https://www.bilibili.com/video/BV1aa411c7mD",
                "https://youtu.be/aaaaaaaaaaa",
                "https://v.qq.com/x/page/bbb.html",
                "https://v.youku.com/v_show/id_XNTk.html",
                "https://evil.example.com/x"
        ));
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).contains("bilibili.com");
    }

    @Test
    void parsesProviderAndVideoId() {
        assertThat(VideoLinks.parse("https://www.bilibili.com/video/BV1GJ411x7h7").provider()).isEqualTo("bilibili");
        assertThat(VideoLinks.parse("https://www.bilibili.com/video/BV1GJ411x7h7").videoId()).isEqualTo("BV1GJ411x7h7");
        assertThat(VideoLinks.parse("https://www.bilibili.com/video/av170001").videoId()).isEqualTo("av170001");
        assertThat(VideoLinks.parse("https://b23.tv/abcd123").provider()).isEqualTo("b23");
        assertThat(VideoLinks.parse("https://youtu.be/dQw4w9WgXcQ").videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(VideoLinks.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ").videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(VideoLinks.parse("https://v.qq.com/x/page/abc123.html").provider()).isEqualTo("tencent");
        assertThat(VideoLinks.parse("https://v.youku.com/v_show/id_XNTk.html").provider()).isEqualTo("youku");
        assertThat(VideoLinks.parse("https://www.douyin.com/video/123456").provider()).isEqualTo("douyin");
    }
}
