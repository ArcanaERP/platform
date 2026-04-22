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
}
