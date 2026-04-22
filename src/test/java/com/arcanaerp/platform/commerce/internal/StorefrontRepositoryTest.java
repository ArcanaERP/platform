package com.arcanaerp.platform.commerce.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class StorefrontRepositoryTest {

    @Autowired
    private StorefrontRepository storefrontRepository;

    @Test
    void filtersStorefrontsByTenantAndActive() {
        storefrontRepository.save(
            Storefront.create("tenant01", "b2c-main", "B2C Main", "USD", "en-US", true, Instant.parse("2026-04-22T01:00:00Z"))
        );
        storefrontRepository.save(
            Storefront.create("tenant01", "wholesale", "Wholesale", "USD", "en-US", false, Instant.parse("2026-04-22T02:00:00Z"))
        );
        storefrontRepository.save(
            Storefront.create("tenant02", "b2c-main", "Other Tenant", "USD", "en-US", true, Instant.parse("2026-04-22T03:00:00Z"))
        );

        var page = storefrontRepository.findByTenantCodeAndActive(
            "TENANT01",
            true,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getStorefrontCode()).isEqualTo("B2C-MAIN");
    }
}
