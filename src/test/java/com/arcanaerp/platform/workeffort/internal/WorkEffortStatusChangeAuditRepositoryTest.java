package com.arcanaerp.platform.workeffort.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class WorkEffortStatusChangeAuditRepositoryTest {

    @Autowired
    private WorkEffortRepository workEffortRepository;

    @Autowired
    private WorkEffortStatusChangeAuditRepository workEffortStatusChangeAuditRepository;

    @Test
    void filtersStatusHistoryByTenantActorAndChangedAtRange() {
        WorkEffort workEffort = workEffortRepository.save(
            WorkEffort.create(
                "tenant01",
                "we-001",
                "Prepare shipment",
                "Prepare shipment for dispatch",
                WorkEffortStatus.PLANNED,
                "agent01@tenant.com",
                null,
                Instant.parse("2026-04-21T10:00:00Z")
            )
        );
        workEffortStatusChangeAuditRepository.save(
            WorkEffortStatusChangeAudit.create(
                workEffort.getId(),
                WorkEffortStatus.PLANNED,
                WorkEffortStatus.IN_PROGRESS,
                "tenant01",
                "Started work",
                "agent01@tenant.com",
                Instant.parse("2026-04-22T10:00:00Z")
            )
        );
        workEffortStatusChangeAuditRepository.save(
            WorkEffortStatusChangeAudit.create(
                workEffort.getId(),
                WorkEffortStatus.IN_PROGRESS,
                WorkEffortStatus.COMPLETED,
                "tenant01",
                "Finished work",
                "agent02@tenant.com",
                Instant.parse("2026-04-22T11:00:00Z")
            )
        );

        var page = workEffortStatusChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            "TENANT01",
            "agent01@tenant.com",
            Instant.parse("2026-04-22T09:00:00Z"),
            Instant.parse("2026-04-22T10:30:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCurrentStatus()).isEqualTo(WorkEffortStatus.IN_PROGRESS);
    }
}
