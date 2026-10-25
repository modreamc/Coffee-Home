package com.forum.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.config.WebConfig;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class PostTagTest {
    private final PostService service = new PostService(
            null, null, null, null, null, null, null,
            new ObjectMapper(), new HtmlSanitizer(), null, new WebConfig("./picture"));

    @Test
    void serializesTagsDeduplicatedAndCapped() {
        String json = service.serializeTags(List.of(" 攻略 ", "攻略", "只狼", "老头环", "多余标签"));
        assertThat(json).isEqualTo("[\"攻略\",\"只狼\",\"老头环\"]");
    }

    @Test
    void blankTagsProduceNull() {
        assertThat(service.serializeTags(List.of("  ", ""))).isNull();
        assertThat(service.serializeTags(null)).isNull();
    }

    @Test
    void parseTagsIsDefensive() {
        assertThat(service.parseTags(null)).isEmpty();
        assertThat(service.parseTags("not-json")).isEmpty();
        assertThat(service.parseTags("[\"a\",\"b\"]")).containsExactly("a", "b");
    }
}
