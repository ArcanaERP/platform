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
class StorefrontProductActivationAuditRepositoryTest {

    @Autowired
    private StorefrontProductActivationAuditRepository storefrontProductActivationAuditRepository;

    @Test
    void filtersActivationHistoryByTenantChangedByCurrentActiveAndChangedAt() {
        UUID storefrontProductId = UUID.randomUUID();
        storefrontProductActivationAuditRepository.save(
            StorefrontProductActivationAudit.create(
                storefrontProductId,
                true,
                false,
                "Seasonal removal",
                "TENANT01",
                "actor.one@arcanaerp.com",
                Instant.parse("2026-04-22T01:00:00Z")
            )
        );
        storefrontProductActivationAuditRepository.save(
            StorefrontProductActivationAudit.create(
                storefrontProductId,
                false,
                true,
                "Restored to shelf",
                "TENANT02",
                "actor.two@arcanaerp.com",
                Instant.parse("2026-04-22T02:00:00Z")
            )
        );

        var page = storefrontProductActivationAuditRepository.findHistoryFiltered(
            storefrontProductId,
            "TENANT02",
            "actor.two@arcanaerp.com",
            true,
            Instant.parse("2026-04-22T01:30:00Z"),
            Instant.parse("2026-04-22T02:30:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).isCurrentActive()).isTrue();
        assertThat(page.getContent().get(0).getChangedBy()).isEqualTo("actor.two@arcanaerp.com");
    }
}
