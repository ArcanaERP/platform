package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CollectionsAssignmentRepositoryTest {

    @Autowired
    private CollectionsAssignmentRepository collectionsAssignmentRepository;

    @Test
    void enforcesUniqueInvoiceNumber() {
        collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "TENANT-01",
            "INV-3000",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));

        assertThatThrownBy(() -> collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "TENANT-01",
            "INV-3000",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:01:00Z")
        )))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsAssignmentsByInvoiceNumberIn() {
        collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "TENANT-02",
            "INV-3001",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "TENANT-02",
            "INV-3002",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:01:00Z")
        ));

        List<CollectionsAssignment> assignments = collectionsAssignmentRepository.findByInvoiceNumberIn(
            List.of("INV-3002", "INV-3001")
        );

        assertThat(assignments).hasSize(2);
        assertThat(assignments).extracting(CollectionsAssignment::getInvoiceNumber)
            .containsExactlyInAnyOrder("INV-3001", "INV-3002");
    }

    @Test
    void persistsFollowUpSchedulingFields() {
        CollectionsAssignment assignment = collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "TENANT-03",
            "INV-3003",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ).scheduleFollowUp(
            Instant.parse("2026-03-13T09:00:00Z"),
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T01:00:00Z")
        ));

        CollectionsAssignment reloaded = collectionsAssignmentRepository.findByInvoiceNumber(assignment.getInvoiceNumber())
            .orElseThrow();

        assertThat(reloaded.getFollowUpAt()).isEqualTo(Instant.parse("2026-03-13T09:00:00Z"));
        assertThat(reloaded.getFollowUpSetBy()).isEqualTo("manager@arcanaerp.com");
        assertThat(reloaded.getFollowUpSetAt()).isEqualTo(Instant.parse("2026-03-12T01:00:00Z"));
    }
}
