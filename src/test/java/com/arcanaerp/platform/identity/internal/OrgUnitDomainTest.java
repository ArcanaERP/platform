package com.arcanaerp.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrgUnitDomainTest {

    @Test
    void createNormalizesCodeAndName() {
        OrgUnit orgUnit = OrgUnit.create(
            UUID.randomUUID(),
            "  ops  ",
            "  Operations  ",
            Instant.parse("2026-02-28T00:00:00Z")
        );

        assertThat(orgUnit.getCode()).isEqualTo("OPS");
        assertThat(orgUnit.getName()).isEqualTo("Operations");
        assertThat(orgUnit.isActive()).isTrue();
    }

    @Test
    void createRequiresTenantId() {
        assertThatThrownBy(() ->
            OrgUnit.create(null, "OPS", "Operations", Instant.parse("2026-02-28T00:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("tenantId is required");
    }
}
