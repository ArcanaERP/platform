package com.arcanaerp.platform.products.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PriceRepositoryTest {

    @Autowired
    private PriceRepository priceRepository;

    @Test
    void findsLatestPriceByEffectiveFrom() {
        UUID productId = UUID.randomUUID();
        priceRepository.save(
            Price.create(productId, new BigDecimal("9.99"), "USD", Instant.parse("2026-01-01T00:00:00Z"))
        );
        priceRepository.save(
            Price.create(productId, new BigDecimal("12.49"), "USD", Instant.parse("2026-02-01T00:00:00Z"))
        );

        Price latest = priceRepository.findTopByProductIdOrderByEffectiveFromDesc(productId).orElseThrow();

        assertThat(latest.getAmount()).isEqualByComparingTo("12.49");
        assertThat(latest.getCurrencyCode()).isEqualTo("USD");
    }
}
