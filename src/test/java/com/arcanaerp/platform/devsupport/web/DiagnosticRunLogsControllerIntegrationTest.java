package com.arcanaerp.platform.devsupport.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosticRunLogsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsReadsAndListsDiagnosticRunLogs() throws Exception {
        DiagnosticRunLogsWebIntegrationTestSupport.createDiagnosticRunLog(
            mockMvc,
            "devdiag01",
            "run-001",
            "db-health",
            "Database Health Check",
            "Connection pool stable",
            DiagnosticRunStatus.PASSED,
            "2026-04-25T01:00:00Z",
            "2026-04-25T01:05:00Z"
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantCode").value("DEVDIAG01"))
            .andExpect(jsonPath("$.runNumber").value("RUN-001"))
            .andExpect(jsonPath("$.diagnosticCode").value("DB-HEALTH"))
            .andExpect(jsonPath("$.status").value("PASSED"));

        DiagnosticRunLogsWebIntegrationTestSupport.createDiagnosticRunLog(
            mockMvc,
            "devdiag01",
            "run-002",
            "cache-health",
            "Cache Health Check",
            "Latency elevated",
            DiagnosticRunStatus.WARNING,
            "2026-04-24T01:00:00Z",
            "2026-04-24T01:10:00Z"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(DiagnosticRunLogsWebIntegrationTestSupport.getDiagnosticRunLogRequest("devdiag01", "run-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runNumber").value("RUN-001"))
            .andExpect(jsonPath("$.title").value("Database Health Check"));

        mockMvc.perform(
            DiagnosticRunLogsWebIntegrationTestSupport.listDiagnosticRunLogsRequest(
                "devdiag01",
                0,
                10,
                "status", "PASSED",
                "startedAtFrom", "2026-04-25T00:00:00Z",
                "startedAtTo", "2026-04-26T00:00:00Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[?(@.runNumber=='RUN-001')].title", hasItem("Database Health Check")));
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        DiagnosticRunLogsWebIntegrationTestSupport.createDiagnosticRunLog(
            mockMvc,
            "devdiag02",
            "run-001",
            "db-health",
            "Database Health Check",
            "Connection pool stable",
            DiagnosticRunStatus.PASSED,
            "2026-04-25T01:00:00Z",
            "2026-04-25T01:05:00Z"
        )
            .andExpect(status().isCreated());

        mockMvc.perform(DiagnosticRunLogsWebIntegrationTestSupport.listDiagnosticRunLogsRequest("devdiag02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.runNumber=='RUN-001')].title", hasItem("Database Health Check")));
    }

    @Test
    void rejectsDuplicateTenantLocalRunNumbers() throws Exception {
        DiagnosticRunLogsWebIntegrationTestSupport.createDiagnosticRunLog(
            mockMvc,
            "devdiag03",
            "run-001",
            "db-health",
            "Database Health Check",
            "Connection pool stable",
            DiagnosticRunStatus.PASSED,
            "2026-04-25T01:00:00Z",
            "2026-04-25T01:05:00Z"
        )
            .andExpect(status().isCreated());

        DiagnosticRunLogsWebIntegrationTestSupport.createDiagnosticRunLog(
            mockMvc,
            "devdiag03",
            "RUN-001",
            "db-health",
            "Duplicate Run",
            "Duplicate entry",
            DiagnosticRunStatus.FAILED,
            "2026-04-25T02:00:00Z",
            "2026-04-25T02:05:00Z"
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Diagnostic run log already exists for tenant/runNumber: DEVDIAG03/RUN-001"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/diagnostic-run-logs"));
    }

    @Test
    void returnsNotFoundForMissingDiagnosticRunLog() throws Exception {
        mockMvc.perform(DiagnosticRunLogsWebIntegrationTestSupport.getDiagnosticRunLogRequest("devdiag04", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Diagnostic run log not found for tenant/runNumber: DEVDIAG04/MISSING"))
            .andExpect(jsonPath("$.path").value("/api/dev-support/diagnostic-run-logs/missing"));
    }

    @Test
    void rejectsInvalidDateFiltersAndRanges() throws Exception {
        mockMvc.perform(
            DiagnosticRunLogsWebIntegrationTestSupport.listDiagnosticRunLogsRequest(
                "devdiag05",
                0,
                10,
                "startedAtFrom", "not-a-timestamp"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("startedAtFrom query parameter must be a valid ISO-8601 instant"));

        mockMvc.perform(
            DiagnosticRunLogsWebIntegrationTestSupport.listDiagnosticRunLogsRequest(
                "devdiag05",
                0,
                10,
                "startedAtFrom", "2026-04-26T00:00:00Z",
                "startedAtTo", "2026-04-25T00:00:00Z"
            )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("startedAtFrom must be before or equal to startedAtTo"));
    }
}
