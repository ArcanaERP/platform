package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CollectionsAssignmentDomainTest {

    @Test
    void createNormalizesAssignmentFields() {
        CollectionsAssignment assignment = CollectionsAssignment.create(
            " tenant-01 ",
            " inv-1000 ",
            " Collector@ArcanaERP.com ",
            " Manager@ArcanaERP.com ",
            Instant.parse("2026-03-12T00:00:00Z")
        );

        assertThat(assignment.getTenantCode()).isEqualTo("TENANT-01");
        assertThat(assignment.getInvoiceNumber()).isEqualTo("INV-1000");
        assertThat(assignment.getAssignedTo()).isEqualTo("collector@arcanaerp.com");
        assertThat(assignment.getAssignedBy()).isEqualTo("manager@arcanaerp.com");
    }

    @Test
    void createRejectsInvalidAssignedTo() {
        assertThatThrownBy(() -> CollectionsAssignment.create(
            "TENANT-01",
            "INV-1001",
            "not-an-email",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("assignedTo is invalid");
    }

    @Test
    void auditCreateNormalizesAssignmentFields() {
        CollectionsAssignmentAudit audit = CollectionsAssignmentAudit.create(
            " tenant-01 ",
            " inv-1000 ",
            " Collector@ArcanaERP.com ",
            " Manager@ArcanaERP.com ",
            Instant.parse("2026-03-12T00:00:00Z")
        );

        assertThat(audit.getTenantCode()).isEqualTo("TENANT-01");
        assertThat(audit.getInvoiceNumber()).isEqualTo("INV-1000");
        assertThat(audit.getAssignedTo()).isEqualTo("collector@arcanaerp.com");
        assertThat(audit.getAssignedBy()).isEqualTo("manager@arcanaerp.com");
    }

    @Test
    void scheduleFollowUpNormalizesScheduler() {
        CollectionsAssignment assignment = CollectionsAssignment.create(
            "TENANT-01",
            "INV-1002",
            "collector@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        );

        assignment.scheduleFollowUp(
            Instant.parse("2026-03-13T00:00:00Z"),
            " Manager@ArcanaERP.com ",
            Instant.parse("2026-03-12T01:00:00Z")
        );

        assertThat(assignment.getFollowUpAt()).isEqualTo(Instant.parse("2026-03-13T00:00:00Z"));
        assertThat(assignment.getFollowUpSetBy()).isEqualTo("manager@arcanaerp.com");
        assertThat(assignment.getFollowUpSetAt()).isEqualTo(Instant.parse("2026-03-12T01:00:00Z"));
    }
}
