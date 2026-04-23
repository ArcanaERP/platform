package com.arcanaerp.platform.devsupport.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class DiagnosticRunLogRepositoryTest {

    @Autowired
    private DiagnosticRunLogRepository diagnosticRunLogRepository;

    @Test
    void filtersDiagnosticRunLogsByTenantStatusAndStartedAtRange() {
        diagnosticRunLogRepository.save(
            DiagnosticRunLog.create(
                "tenant01",
                "run-001",
                "db-health",
                "Database Health Check",
                "Connection pool stable",
                DiagnosticRunStatus.PASSED,
                Instant.parse("2026-04-25T01:00:00Z"),
                Instant.parse("2026-04-25T01:05:00Z"),
                Instant.parse("2026-04-25T01:06:00Z")
            )
        );
        diagnosticRunLogRepository.save(
            DiagnosticRunLog.create(
                "tenant01",
                "run-002",
                "cache-health",
                "Cache Health Check",
                "Latency elevated",
                DiagnosticRunStatus.WARNING,
                Instant.parse("2026-04-24T01:00:00Z"),
                Instant.parse("2026-04-24T01:10:00Z"),
                Instant.parse("2026-04-24T01:11:00Z")
            )
        );
        diagnosticRunLogRepository.save(
            DiagnosticRunLog.create(
                "tenant02",
                "run-003",
                "db-health",
                "Other Tenant Check",
                "Different tenant",
                DiagnosticRunStatus.PASSED,
                Instant.parse("2026-04-25T01:00:00Z"),
                Instant.parse("2026-04-25T01:05:00Z"),
                Instant.parse("2026-04-25T01:06:00Z")
            )
        );

        var page = diagnosticRunLogRepository.findFiltered(
            "TENANT01",
            DiagnosticRunStatus.PASSED,
            Instant.parse("2026-04-25T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getRunNumber()).isEqualTo("RUN-001");
    }
}
