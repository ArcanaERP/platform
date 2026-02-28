package com.arcanaerp.platform.core.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void usesDefaultsWhenInputIsNull() {
        PageQuery pageQuery = PageQuery.of(null, null);

        assertThat(pageQuery.page()).isEqualTo(PageQuery.DEFAULT_PAGE);
        assertThat(pageQuery.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
    }

    @Test
    void rejectsInvalidPageAndSize() {
        assertThatThrownBy(() -> new PageQuery(-1, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("page must be greater than or equal to zero");

        assertThatThrownBy(() -> new PageQuery(0, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("size must be between 1 and 100");
    }
}
