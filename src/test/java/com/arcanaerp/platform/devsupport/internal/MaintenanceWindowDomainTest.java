package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MaintenanceWindowDomainTest {

    @Test
    void normalizesMaintenanceWindowFields() {
        MaintenanceWindow window = MaintenanceWindow.create(
            "tenant01",
            "mw-001",
            " Planned Maintenance ",
            " Database upgrade ",
            Instant.parse("2026-04-23T01:00:00Z"),
            Instant.parse("2026-04-23T03:00:00Z"),
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        );

        assertThat(window.getTenantCode()).isEqualTo("TENANT01");
        assertThat(window.getWindowCode()).isEqualTo("MW-001");
        assertThat(window.getTitle()).isEqualTo("Planned Maintenance");
        assertThat(window.getDescription()).isEqualTo("Database upgrade");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> MaintenanceWindow.create(
            "tenant01",
            "mw-001",
            "Planned Maintenance",
            "Database upgrade",
            Instant.parse("2026-04-23T03:00:00Z"),
            Instant.parse("2026-04-23T01:00:00Z"),
            true,
            Instant.parse("2026-04-22T01:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("endsAt must be after startsAt");
    }
}
