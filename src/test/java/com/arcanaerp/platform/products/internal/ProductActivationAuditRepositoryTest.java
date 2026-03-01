package com.arcanaerp.platform.products.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

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
                "TENANT01",
                "product-admin@arcanaerp.com",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        );
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                false,
                true,
                "Reactivated for customer demand",
                "TENANT01",
                "operations@arcanaerp.com",
                Instant.parse("2026-02-28T01:00:00Z")
            )
        );

        ProductActivationAudit latest = productActivationAuditRepository
            .findTopByProductIdOrderByChangedAtDesc(productId)
            .orElseThrow();

        assertThat(latest.isCurrentActive()).isTrue();
        assertThat(latest.getReason()).isEqualTo("Reactivated for customer demand");
        assertThat(latest.getTenantCode()).isEqualTo("TENANT01");
        assertThat(latest.getChangedBy()).isEqualTo("operations@arcanaerp.com");
    }

    @Test
    void filtersActivationAuditsByTenantCode() {
        UUID productId = UUID.randomUUID();
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                true,
                false,
                "Tenant A deactivation",
                "TENANTA",
                "a@arcanaerp.com",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        );
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                false,
                true,
                "Tenant B reactivation",
                "TENANTB",
                "b@arcanaerp.com",
                Instant.parse("2026-02-28T01:00:00Z")
            )
        );

        var tenantAPage = productActivationAuditRepository.findByProductIdAndTenantCode(
            productId,
            "TENANTA",
            PageRequest.of(0, 10)
        );
        var tenantBPage = productActivationAuditRepository.findByProductIdAndTenantCode(
            productId,
            "TENANTB",
            PageRequest.of(0, 10)
        );

        assertThat(tenantAPage.getTotalElements()).isEqualTo(1);
        assertThat(tenantAPage.getContent().getFirst().getReason()).isEqualTo("Tenant A deactivation");
        assertThat(tenantBPage.getTotalElements()).isEqualTo(1);
        assertThat(tenantBPage.getContent().getFirst().getReason()).isEqualTo("Tenant B reactivation");
    }

    @Test
    void filtersActivationAuditsByChangedBy() {
        UUID productId = UUID.randomUUID();
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                true,
                false,
                "Actor A deactivation",
                "TENANTA",
                "actor-a@arcanaerp.com",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        );
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                false,
                true,
                "Actor B reactivation",
                "TENANTB",
                "actor-b@arcanaerp.com",
                Instant.parse("2026-02-28T01:00:00Z")
            )
        );

        var actorAPage = productActivationAuditRepository.findByProductIdAndChangedBy(
            productId,
            "actor-a@arcanaerp.com",
            PageRequest.of(0, 10)
        );
        var combinedPage = productActivationAuditRepository.findByProductIdAndTenantCodeAndChangedBy(
            productId,
            "TENANTB",
            "actor-b@arcanaerp.com",
            PageRequest.of(0, 10)
        );

        assertThat(actorAPage.getTotalElements()).isEqualTo(1);
        assertThat(actorAPage.getContent().getFirst().getReason()).isEqualTo("Actor A deactivation");
        assertThat(combinedPage.getTotalElements()).isEqualTo(1);
        assertThat(combinedPage.getContent().getFirst().getReason()).isEqualTo("Actor B reactivation");
    }

    @Test
    void filtersActivationAuditsByCurrentActive() {
        UUID productId = UUID.randomUUID();
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                true,
                false,
                "Deactivated",
                "TENANTA",
                "actor-a@arcanaerp.com",
                Instant.parse("2026-02-28T00:00:00Z")
            )
        );
        productActivationAuditRepository.save(
            ProductActivationAudit.create(
                productId,
                false,
                true,
                "Reactivated",
                "TENANTB",
                "actor-b@arcanaerp.com",
                Instant.parse("2026-02-28T01:00:00Z")
            )
        );

        var activePage = productActivationAuditRepository.findHistoryFiltered(
            productId,
            null,
            null,
            true,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var inactivePage = productActivationAuditRepository.findHistoryFiltered(
            productId,
            null,
            null,
            false,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var combinedPage = productActivationAuditRepository.findHistoryFiltered(
            productId,
            "TENANTB",
            "actor-b@arcanaerp.com",
            true,
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertThat(activePage.getTotalElements()).isEqualTo(1);
        assertThat(activePage.getContent().getFirst().getReason()).isEqualTo("Reactivated");
        assertThat(inactivePage.getTotalElements()).isEqualTo(1);
        assertThat(inactivePage.getContent().getFirst().getReason()).isEqualTo("Deactivated");
        assertThat(combinedPage.getTotalElements()).isEqualTo(1);
        assertThat(combinedPage.getContent().getFirst().getReason()).isEqualTo("Reactivated");
    }
}
