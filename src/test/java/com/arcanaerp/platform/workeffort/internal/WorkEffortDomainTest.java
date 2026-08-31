package com.arcanaerp.platform.workeffort.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkEffortDomainTest {

    @Test
    void normalizesWorkEffortFields() {
        WorkEffort workEffort = WorkEffort.create(
            "tenant01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            WorkEffortStatus.PLANNED,
            "Agent01@tenant.com",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-21T10:00:00Z")
        );

        assertThat(workEffort.getTenantCode()).isEqualTo("TENANT01");
        assertThat(workEffort.getEffortNumber()).isEqualTo("WE-001");
        assertThat(workEffort.getAssignedTo()).isEqualTo("agent01@tenant.com");
    }

    @Test
    void rejectsMissingStatus() {
        assertThatThrownBy(() -> WorkEffort.create(
            "tenant01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            null,
            "agent01@tenant.com",
            null,
            Instant.parse("2026-04-21T10:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("status is required");
    }

    @Test
    void transitionsWorkEffortStatus() {
        WorkEffort workEffort = WorkEffort.create(
            "tenant01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            WorkEffortStatus.PLANNED,
            "agent01@tenant.com",
            null,
            Instant.parse("2026-04-21T10:00:00Z")
        );

        workEffort.transitionTo(WorkEffortStatus.IN_PROGRESS);
        workEffort.transitionTo(WorkEffortStatus.COMPLETED);

        assertThat(workEffort.getStatus()).isEqualTo(WorkEffortStatus.COMPLETED);
    }

    @Test
    void assignsWorkEffortToNormalizedActor() {
        WorkEffort workEffort = WorkEffort.create(
            "tenant01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            WorkEffortStatus.PLANNED,
            "agent01@tenant.com",
            null,
            Instant.parse("2026-04-21T10:00:00Z")
        );

        workEffort.assignTo("Agent02@Tenant.com");

        assertThat(workEffort.getAssignedTo()).isEqualTo("agent02@tenant.com");
    }

    @Test
    void assignmentAuditCreateNormalizesFields() {
        WorkEffortAssignmentChangeAudit audit = WorkEffortAssignmentChangeAudit.create(
            java.util.UUID.randomUUID(),
            "Agent01@Tenant.com",
            "Agent02@Tenant.com",
            "tenant01",
            "Coverage handoff",
            "Manager@Tenant.com",
            Instant.parse("2026-04-22T10:00:00Z")
        );

        assertThat(audit.getTenantCode()).isEqualTo("TENANT01");
        assertThat(audit.getPreviousAssignedTo()).isEqualTo("agent01@tenant.com");
        assertThat(audit.getCurrentAssignedTo()).isEqualTo("agent02@tenant.com");
        assertThat(audit.getAssignedBy()).isEqualTo("manager@tenant.com");
    }

    @Test
    void rejectsInvalidStatusTransition() {
        WorkEffort workEffort = WorkEffort.create(
            "tenant01",
            "we-001",
            "Prepare shipment",
            "Prepare shipment for dispatch",
            WorkEffortStatus.COMPLETED,
            "agent01@tenant.com",
            null,
            Instant.parse("2026-04-21T10:00:00Z")
        );

        assertThatThrownBy(() -> workEffort.transitionTo(WorkEffortStatus.IN_PROGRESS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Completed work efforts cannot change status");
    }
}
