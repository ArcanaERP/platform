package com.arcanaerp.platform.commerce.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class StorefrontProductRepositoryTest {

    @Autowired
    private StorefrontProductRepository storefrontProductRepository;

    @Test
    void filtersStorefrontProductsByTenantStorefrontAndActive() {
        UUID storefrontId = UUID.randomUUID();
        storefrontProductRepository.save(
            StorefrontProduct.create(
                storefrontId,
                "tenant01",
                "b2c-main",
                "ARC-100",
                "Featured Product",
                1,
                true,
                Instant.parse("2026-04-22T01:00:00Z")
            )
        );
        storefrontProductRepository.save(
            StorefrontProduct.create(
                storefrontId,
                "tenant01",
                "b2c-main",
                "ARC-200",
                "Inactive Product",
                2,
                false,
                Instant.parse("2026-04-22T02:00:00Z")
            )
        );
        storefrontProductRepository.save(
            StorefrontProduct.create(
                UUID.randomUUID(),
                "tenant02",
                "b2c-main",
                "ARC-300",
                "Other Tenant",
                1,
                true,
                Instant.parse("2026-04-22T03:00:00Z")
            )
        );

        var page = storefrontProductRepository.findByTenantCodeAndStorefrontCodeAndActive(
            "TENANT01",
            "B2C-MAIN",
            true,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "position"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getSku()).isEqualTo("ARC-100");
    }
}
