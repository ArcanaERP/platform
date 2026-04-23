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
class DiagnosticRunLogCatalogIntegrationTest {

    @Autowired
    private DevSupportCatalog devSupportCatalog;

    @Test
    void registersReadsAndListsDiagnosticRunLogs() {
        DiagnosticRunLogView created = devSupportCatalog.registerDiagnosticRunLog(
            new RegisterDiagnosticRunLogCommand(
                "devsupportdiag01",
                "run-001",
                "db-health",
                "Database Health Check",
                "Connection pool stable",
                DiagnosticRunStatus.PASSED,
                Instant.parse("2026-04-25T01:00:00Z"),
                Instant.parse("2026-04-25T01:05:00Z")
            )
        );
        devSupportCatalog.registerDiagnosticRunLog(
            new RegisterDiagnosticRunLogCommand(
                "devsupportdiag01",
                "run-002",
                "cache-health",
                "Cache Health Check",
                "Latency elevated",
                DiagnosticRunStatus.WARNING,
                Instant.parse("2026-04-24T01:00:00Z"),
                Instant.parse("2026-04-24T01:10:00Z")
            )
        );

        DiagnosticRunLogView loaded = devSupportCatalog.getDiagnosticRunLog("devsupportdiag01", "run-001");
        var listed = devSupportCatalog.listDiagnosticRunLogs(
            "devsupportdiag01",
            new PageQuery(0, 10),
            DiagnosticRunStatus.PASSED,
            Instant.parse("2026-04-25T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        assertThat(loaded.runNumber()).isEqualTo(created.runNumber());
        assertThat(loaded.tenantCode()).isEqualTo("DEVSUPPORTDIAG01");
        assertThat(loaded.diagnosticCode()).isEqualTo("DB-HEALTH");
        assertThat(listed.totalItems()).isEqualTo(1);
        assertThat(listed.items()).extracting(DiagnosticRunLogView::runNumber).containsExactly("RUN-001");
    }

    @Test
    void rejectsDuplicateTenantLocalRunNumbers() {
        devSupportCatalog.registerDiagnosticRunLog(
            new RegisterDiagnosticRunLogCommand(
                "devsupportdiag02",
                "run-001",
                "db-health",
                "Database Health Check",
                "Connection pool stable",
                DiagnosticRunStatus.PASSED,
                Instant.parse("2026-04-25T01:00:00Z"),
                Instant.parse("2026-04-25T01:05:00Z")
            )
        );

        assertThatThrownBy(() -> devSupportCatalog.registerDiagnosticRunLog(
            new RegisterDiagnosticRunLogCommand(
                "devsupportdiag02",
                "RUN-001",
                "db-health",
                "Duplicate Run",
                "Duplicate entry",
                DiagnosticRunStatus.FAILED,
                Instant.parse("2026-04-25T02:00:00Z"),
                Instant.parse("2026-04-25T02:05:00Z")
            )
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Diagnostic run log already exists for tenant/runNumber: DEVSUPPORTDIAG02/RUN-001");
    }

    @Test
    void rejectsMissingDiagnosticRunLogLookup() {
        assertThatThrownBy(() -> devSupportCatalog.getDiagnosticRunLog("devsupportdiag03", "missing"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Diagnostic run log not found for tenant/runNumber: DEVSUPPORTDIAG03/MISSING");
    }
}
