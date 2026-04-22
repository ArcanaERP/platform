package com.arcanaerp.platform.commerce.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StorefrontProductDomainTest {

    @Test
    void normalizesStorefrontProductFields() {
        StorefrontProduct storefrontProduct = StorefrontProduct.create(
            UUID.randomUUID(),
            "tenant01",
            "b2c-main",
            "arc-100",
            "Featured Product",
            2,
            true,
            Instant.parse("2026-04-22T02:00:00Z")
        );

        assertThat(storefrontProduct.getTenantCode()).isEqualTo("TENANT01");
        assertThat(storefrontProduct.getStorefrontCode()).isEqualTo("B2C-MAIN");
        assertThat(storefrontProduct.getSku()).isEqualTo("ARC-100");
        assertThat(storefrontProduct.getMerchandisingName()).isEqualTo("Featured Product");
    }

    @Test
    void rejectsNegativePosition() {
        assertThatThrownBy(() -> StorefrontProduct.create(
            UUID.randomUUID(),
            "tenant01",
            "b2c-main",
            "arc-100",
            null,
            -1,
            true,
            Instant.parse("2026-04-22T02:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("position must be greater than or equal to zero");
    }
}
