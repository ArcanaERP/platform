package com.arcanaerp.platform.commerce.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StorefrontDomainTest {

    @Test
    void normalizesStorefrontFields() {
        Storefront storefront = Storefront.create(
            "tenant01",
            "b2c-main",
            "B2C Main",
            "usd",
            "en-US",
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        );

        assertThat(storefront.getTenantCode()).isEqualTo("TENANT01");
        assertThat(storefront.getStorefrontCode()).isEqualTo("B2C-MAIN");
        assertThat(storefront.getCurrencyCode()).isEqualTo("USD");
        assertThat(storefront.getDefaultLanguageTag()).isEqualTo("en-US");
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThatThrownBy(() -> Storefront.create(
            "tenant01",
            "b2c-main",
            "B2C Main",
            "us",
            "en-US",
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currencyCode must be a 3-letter ISO code");
    }
}
