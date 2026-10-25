package com.forum.util;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class PagingTest {
    @Test
    void clampsPageAndSize() {
        assertThat(Paging.page(-5)).isEqualTo(1);
        assertThat(Paging.page(3)).isEqualTo(3);
        assertThat(Paging.size(0)).isEqualTo(Paging.DEFAULT_SIZE);
        assertThat(Paging.size(-1)).isEqualTo(Paging.DEFAULT_SIZE);
        assertThat(Paging.size(999_999)).isEqualTo(Paging.MAX_SIZE);
        assertThat(Paging.size(15)).isEqualTo(15);
    }

    @Test
    void escapesLikeWildcards() {
        assertThat(Paging.escapeLike("100%")).isEqualTo("100\\%");
        assertThat(Paging.escapeLike("a_b")).isEqualTo("a\\_b");
        assertThat(Paging.escapeLike("normal")).isEqualTo("normal");
    }
}
