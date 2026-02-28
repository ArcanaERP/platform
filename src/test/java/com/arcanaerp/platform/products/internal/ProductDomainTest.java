package com.arcanaerp.platform.products.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductDomainTest {

    @Test
    void categoryCreateNormalizesCodeAndName() {
        Category category = Category.create("  hardware  ", "  Hardware  ", Instant.parse("2026-02-28T00:00:00Z"));

        assertThat(category.getCode()).isEqualTo("HARDWARE");
        assertThat(category.getName()).isEqualTo("Hardware");
    }

    @Test
    void productCreateRequiresCategoryId() {
        assertThatThrownBy(() ->
            Product.create("SKU-1", "Starter Kit", null, Instant.parse("2026-02-28T00:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("categoryId is required");
    }

    @Test
    void priceCreateRejectsNonPositiveAmount() {
        assertThatThrownBy(() ->
            Price.create(UUID.randomUUID(), BigDecimal.ZERO, "USD", Instant.parse("2026-02-28T00:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("amount must be greater than zero");
    }
}
