package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CollectionsAssignmentAuditRepositoryTest {

    @Autowired
    private CollectionsAssignmentAuditRepository collectionsAssignmentAuditRepository;

    @Test
    void listsAssignmentHistoryNewestFirstForInvoice() {
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4000",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4000",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:05:00Z")
        ));
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4001",
            "collector-c@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));

        var page = collectionsAssignmentAuditRepository.findHistoryFiltered(
            "TENANT-01",
            "INV-4000",
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(CollectionsAssignmentAudit::getAssignedTo)
            .containsExactly("collector-b@arcanaerp.com", "collector-a@arcanaerp.com");
    }

    @Test
    void filtersAssignmentHistoryByAssigneeAndAssignedAtRange() {
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4002",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4002",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:05:00Z")
        ));
        collectionsAssignmentAuditRepository.saveAndFlush(CollectionsAssignmentAudit.create(
            "TENANT-01",
            "INV-4002",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));

        var page = collectionsAssignmentAuditRepository.findHistoryFiltered(
            "TENANT-01",
            "INV-4002",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:01:00Z"),
            Instant.parse("2026-03-12T00:10:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "assignedAt"))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getAssignedTo()).isEqualTo("collector-a@arcanaerp.com");
        assertThat(page.getContent().getFirst().getAssignedAt()).isEqualTo(Instant.parse("2026-03-12T00:10:00Z"));
    }
}
