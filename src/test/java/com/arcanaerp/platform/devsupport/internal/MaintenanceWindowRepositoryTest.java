package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class MaintenanceWindowRepositoryTest {

    @Autowired
    private MaintenanceWindowRepository maintenanceWindowRepository;

    @Test
    void filtersMaintenanceWindowsByTenantActiveAndStartRange() {
        maintenanceWindowRepository.save(
            MaintenanceWindow.create(
                "tenant01",
                "mw-001",
                "Planned Maintenance",
                "Database upgrade",
                Instant.parse("2026-04-23T01:00:00Z"),
                Instant.parse("2026-04-23T03:00:00Z"),
                true,
                Instant.parse("2026-04-22T01:00:00Z")
            )
        );
        maintenanceWindowRepository.save(
            MaintenanceWindow.create(
                "tenant01",
                "mw-002",
                "Resolved Maintenance",
                "Earlier patching window",
                Instant.parse("2026-04-20T01:00:00Z"),
                Instant.parse("2026-04-20T02:00:00Z"),
                false,
                Instant.parse("2026-04-19T01:00:00Z")
            )
        );
        maintenanceWindowRepository.save(
            MaintenanceWindow.create(
                "tenant02",
                "mw-003",
                "Other Tenant Window",
                "Other tenant patching",
                Instant.parse("2026-04-23T01:00:00Z"),
                Instant.parse("2026-04-23T02:00:00Z"),
                true,
                Instant.parse("2026-04-22T02:00:00Z")
            )
        );

        var page = maintenanceWindowRepository.findFiltered(
            "TENANT01",
            true,
            Instant.parse("2026-04-22T00:00:00Z"),
            Instant.parse("2026-04-24T00:00:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "startsAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getWindowCode()).isEqualTo("MW-001");
    }
}
