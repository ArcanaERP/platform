package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DiagnosticRunLogDomainTest {

    @Test
    void normalizesDiagnosticRunLogFields() {
        DiagnosticRunLog runLog = DiagnosticRunLog.create(
            "tenant01",
            "run-001",
            "db-health",
            " Database Health Check ",
            " Connection pool stable ",
            DiagnosticRunStatus.PASSED,
            Instant.parse("2026-04-25T01:00:00Z"),
            Instant.parse("2026-04-25T01:05:00Z"),
            Instant.parse("2026-04-25T01:06:00Z")
        );

        assertThat(runLog.getTenantCode()).isEqualTo("TENANT01");
        assertThat(runLog.getRunNumber()).isEqualTo("RUN-001");
        assertThat(runLog.getDiagnosticCode()).isEqualTo("DB-HEALTH");
        assertThat(runLog.getTitle()).isEqualTo("Database Health Check");
        assertThat(runLog.getSummary()).isEqualTo("Connection pool stable");
    }

    @Test
    void rejectsFinishedAtBeforeStartedAt() {
        assertThatThrownBy(() -> DiagnosticRunLog.create(
            "tenant01",
            "run-001",
            "db-health",
            "Database Health Check",
            "Connection pool stable",
            DiagnosticRunStatus.PASSED,
            Instant.parse("2026-04-25T01:05:00Z"),
            Instant.parse("2026-04-25T01:00:00Z"),
            Instant.parse("2026-04-25T01:06:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("finishedAt must be after or equal to startedAt");
    }
}
