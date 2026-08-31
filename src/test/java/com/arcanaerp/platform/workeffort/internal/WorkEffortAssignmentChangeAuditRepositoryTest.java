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
class WorkEffortAssignmentChangeAuditRepositoryTest {

    @Autowired
    private WorkEffortRepository workEffortRepository;

    @Autowired
    private WorkEffortAssignmentChangeAuditRepository workEffortAssignmentChangeAuditRepository;

    @Test
    void filtersAssignmentHistoryByTenantAssigneeActorAndAssignedAtRange() {
        WorkEffort workEffort = seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            "TENANT01",
            "agent02@tenant.com",
            "manager@tenant.com",
            Instant.parse("2026-04-22T09:00:00Z"),
            Instant.parse("2026-04-22T10:30:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCurrentAssignedTo()).isEqualTo("agent02@tenant.com");
    }

    @Test
    void filtersAssignmentHistoryByAssignedTo() {
        WorkEffort workEffort = seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            "TENANT01",
            "agent03@tenant.com",
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(WorkEffortAssignmentChangeAudit::getCurrentAssignedTo)
            .containsExactly("agent03@tenant.com");
    }

    @Test
    void filtersAssignmentHistoryByAssignedBy() {
        WorkEffort workEffort = seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            "TENANT01",
            null,
            "manager@tenant.com",
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(WorkEffortAssignmentChangeAudit::getAssignedBy)
            .containsExactly("manager@tenant.com");
    }

    @Test
    void filtersAssignmentHistoryByAssignedAtRange() {
        WorkEffort workEffort = seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.findHistoryFiltered(
            workEffort.getId(),
            "TENANT01",
            null,
            null,
            Instant.parse("2026-04-22T10:30:00Z"),
            Instant.parse("2026-04-22T11:30:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(WorkEffortAssignmentChangeAudit::getAssignedAt)
            .containsExactly(Instant.parse("2026-04-22T11:00:00Z"));
    }

    @Test
    void summarizesAssignmentActivityByAssignee() {
        seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.summarizeAssignmentActivity(
            "TENANT01",
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "assignedTo"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(
            WorkEffortAssignmentChangeAuditRepository.AssignmentActivitySummaryProjection::getAssignedTo
        ).containsExactly("agent02@tenant.com", "agent03@tenant.com");
        assertThat(page.getContent()).extracting(
            WorkEffortAssignmentChangeAuditRepository.AssignmentActivitySummaryProjection::getAssignmentCount
        ).containsExactly(1L, 1L);
    }

    @Test
    void filtersAssignmentActivitySummaryByAssigneeAndAssignedAtRange() {
        seedAssignmentHistory();

        var page = workEffortAssignmentChangeAuditRepository.summarizeAssignmentActivity(
            "TENANT01",
            "agent03@tenant.com",
            Instant.parse("2026-04-22T10:30:00Z"),
            Instant.parse("2026-04-22T11:30:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "assignedTo"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getAssignedTo()).isEqualTo("agent03@tenant.com");
        assertThat(page.getContent().get(0).getAssignmentCount()).isEqualTo(1);
        assertThat(page.getContent().get(0).getFirstAssignedAt()).isEqualTo(Instant.parse("2026-04-22T11:00:00Z"));
        assertThat(page.getContent().get(0).getLastAssignedAt()).isEqualTo(Instant.parse("2026-04-22T11:00:00Z"));
    }

    private WorkEffort seedAssignmentHistory() {
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
        workEffortAssignmentChangeAuditRepository.save(
            WorkEffortAssignmentChangeAudit.create(
                workEffort.getId(),
                "agent01@tenant.com",
                "agent02@tenant.com",
                "tenant01",
                "Coverage handoff",
                "manager@tenant.com",
                Instant.parse("2026-04-22T10:00:00Z")
            )
        );
        workEffortAssignmentChangeAuditRepository.save(
            WorkEffortAssignmentChangeAudit.create(
                workEffort.getId(),
                "agent02@tenant.com",
                "agent03@tenant.com",
                "tenant01",
                "Escalation",
                "lead@tenant.com",
                Instant.parse("2026-04-22T11:00:00Z")
            )
        );
        return workEffort;
    }
}
