package com.arcanaerp.platform.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrgUnitRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrgUnitRepository orgUnitRepository;

    @Test
    void findsOrgUnitByTenantAndCode() {
        Instant now = Instant.parse("2026-02-28T00:00:00Z");
        Tenant tenant = tenantRepository.save(Tenant.create("acme", "Acme Corp", now));
        orgUnitRepository.save(OrgUnit.create(tenant.getId(), "ops", "Operations", now));

        OrgUnit orgUnit = orgUnitRepository.findByTenantIdAndCode(tenant.getId(), "OPS").orElseThrow();

        assertThat(orgUnit.getName()).isEqualTo("Operations");
    }
}
