package com.arcanaerp.platform.devsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MaintenanceWindowCatalogIntegrationTest {

    @Autowired
    private DevSupportCatalog devSupportCatalog;

    @Test
    void registersReadsAndListsMaintenanceWindows() {
        MaintenanceWindowView created = devSupportCatalog.registerMaintenanceWindow(
            new RegisterMaintenanceWindowCommand(
                "devsupportmw01",
                "mw-001",
                "Planned Maintenance",
                "Database upgrade",
                Instant.parse("2026-04-23T01:00:00Z"),
                Instant.parse("2026-04-23T03:00:00Z"),
                true
            )
        );
        devSupportCatalog.registerMaintenanceWindow(
            new RegisterMaintenanceWindowCommand(
                "devsupportmw01",
                "mw-002",
                "Past Maintenance",
                "Earlier patching",
                Instant.parse("2026-04-20T01:00:00Z"),
                Instant.parse("2026-04-20T02:00:00Z"),
                false
            )
        );

        MaintenanceWindowView loaded = devSupportCatalog.getMaintenanceWindow("devsupportmw01", "mw-001");
        var listed = devSupportCatalog.listMaintenanceWindows(
            "devsupportmw01",
            new PageQuery(0, 10),
            true,
            Instant.parse("2026-04-22T00:00:00Z"),
            Instant.parse("2026-04-24T00:00:00Z")
        );

        assertThat(loaded.windowCode()).isEqualTo(created.windowCode());
        assertThat(loaded.tenantCode()).isEqualTo("DEVSUPPORTMW01");
        assertThat(loaded.startsAt()).isEqualTo(Instant.parse("2026-04-23T01:00:00Z"));
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(MaintenanceWindowView::windowCode).containsExactly("MW-001");
    }

    @Test
    void rejectsDuplicateTenantLocalWindowCodes() {
        devSupportCatalog.registerMaintenanceWindow(
            new RegisterMaintenanceWindowCommand(
                "devsupportmw02",
                "mw-001",
                "Planned Maintenance",
                "Database upgrade",
                Instant.parse("2026-04-23T01:00:00Z"),
                Instant.parse("2026-04-23T03:00:00Z"),
                true
            )
        );

        assertThatThrownBy(() -> devSupportCatalog.registerMaintenanceWindow(
            new RegisterMaintenanceWindowCommand(
                "devsupportmw02",
                "MW-001",
                "Duplicate Window",
                "Duplicate entry",
                Instant.parse("2026-04-24T01:00:00Z"),
                Instant.parse("2026-04-24T03:00:00Z"),
                true
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Maintenance window already exists for tenant/code: DEVSUPPORTMW02/MW-001");
    }

    @Test
    void rejectsMissingMaintenanceWindowLookup() {
        assertThatThrownBy(() -> devSupportCatalog.getMaintenanceWindow("devsupportmw03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Maintenance window not found for tenant/code: DEVSUPPORTMW03/MISSING");
    }
}
