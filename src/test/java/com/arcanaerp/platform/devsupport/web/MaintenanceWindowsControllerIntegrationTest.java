package com.arcanaerp.platform.devsupport.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MaintenanceWindowsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsMaintenanceWindows() throws Exception {
        MaintenanceWindowsWebIntegrationTestSupport.createMaintenanceWindow(
            mockMvc,
            "devmw01",
            "mw-001",
            "Planned Maintenance",
            "Database upgrade",
            "2026-04-23T01:00:00Z",
            "2026-04-23T03:00:00Z",
            true
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("DEVMW01"))
            .andExpect(jsonPath("$.windowCode").value("MW-001"))
            .andExpect(jsonPath("$.active").value(true));

        MaintenanceWindowsWebIntegrationTestSupport.createMaintenanceWindow(
            mockMvc,
            "devmw01",
            "mw-002",
            "Past Maintenance",
            "Earlier patching",
            "2026-04-20T01:00:00Z",
            "2026-04-20T02:00:00Z",
            false
        )
            .andExpect(status().isCreated());

        mockMvc.perform(MaintenanceWindowsWebIntegrationTestSupport.getMaintenanceWindowRequest("devmw01", "mw-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windowCode").value("MW-001"))
            .andExpect(jsonPath("$.title").value("Planned Maintenance"));

        mockMvc.perform(
            MaintenanceWindowsWebIntegrationTestSupport.listMaintenanceWindowsRequest(
                "devmw01",
                0,
                10,
                "active", "true",
                "startsAtFrom", "2026-04-22T00:00:00Z",
                "startsAtTo", "2026-04-24T00:00:00Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.windowCode=='MW-001')].title", hasItem("Planned Maintenance")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        MaintenanceWindowsWebIntegrationTestSupport.createMaintenanceWindow(
            mockMvc,
            "devmw02",
            "mw-001",
            "Planned Maintenance",
            "Quarterly patching",
            "2026-04-24T01:00:00Z",
            "2026-04-24T03:00:00Z",
            true
        )
            .andExpect(status().isCreated());

        mockMvc.perform(MaintenanceWindowsWebIntegrationTestSupport.listMaintenanceWindowsRequest("devmw02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.windowCode=='MW-001')].title", hasItem("Planned Maintenance")));
    }

    @Test
    void rejectsDuplicateTenantLocalWindowCodes() throws Exception {
        MaintenanceWindowsWebIntegrationTestSupport.createMaintenanceWindow(
            mockMvc,
            "devmw03",
            "mw-001",
            "Planned Maintenance",
            "Database upgrade",
            "2026-04-23T01:00:00Z",
            "2026-04-23T03:00:00Z",
            true
        )
            .andExpect(status().isCreated());

        MaintenanceWindowsWebIntegrationTestSupport.createMaintenanceWindow(
            mockMvc,
            "devmw03",
            "MW-001",
            "Duplicate Window",
            "Duplicate entry",
            "2026-04-24T01:00:00Z",
            "2026-04-24T03:00:00Z",
            true
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Maintenance window already exists for tenant/code: DEVMW03/MW-001"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/maintenance-windows"));
    }

    @Test
    void returnsNotFoundForMissingMaintenanceWindow() throws Exception {
        mockMvc.perform(MaintenanceWindowsWebIntegrationTestSupport.getMaintenanceWindowRequest("devmw04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Maintenance window not found for tenant/code: DEVMW04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/maintenance-windows/missing"));
    }

    @Test
    void rejectsInvalidDateFiltersAndRanges() throws Exception {
        mockMvc.perform(
            MaintenanceWindowsWebIntegrationTestSupport.listMaintenanceWindowsRequest(
                "devmw05",
                0,
                10,
                "startsAtFrom", "not-a-timestamp"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("startsAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(
            MaintenanceWindowsWebIntegrationTestSupport.listMaintenanceWindowsRequest(
                "devmw05",
                0,
                10,
                "startsAtFrom", "2026-04-24T00:00:00Z",
                "startsAtTo", "2026-04-23T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("startsAtFrom must be before or equal to startsAtTo"));
    }
}
