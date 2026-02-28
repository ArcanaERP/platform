package com.arcanaerp.platform.products.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProductActivationAuditRepositoryTest {

    @Autowired
    private ProductActivationAuditRepository productActivationAuditRepository;

    @Test
    void findsLatestActivationAuditByProductId() {
        UUID productId = UUID.randomUUID();
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                true,
                false,
                "Discontinued",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        );
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                false,
                true,
                "Reactivated for customer demand",
                Instant.parse("2026-02-28T01:00:00Z")
            )
        );

        ProductActivationAudit latest = productActivationAuditRepository
            .findTopByProductIdOrderByChangedAtDesc(productId)
            .orElseThrow();

        assertThat(latest.isCurrentActive()).isTrue();
        assertThat(latest.getReason()).isEqualTo("Reactivated for customer demand");
    }
}
