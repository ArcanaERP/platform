package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CollectionsAssignmentReleaseAuditRepositoryTest {

    @Autowired
    private CollectionsAssignmentReleaseAuditRepository collectionsAssignmentReleaseAuditRepository;

    @Test
    void listsReleaseHistoryNewestFirstForInvoice() {
        collectionsAssignmentReleaseAuditRepository.saveAndFlush(CollectionsAssignmentReleaseAudit.create(
            "TENANT-01",
            "INV-7000",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z"),
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));
        collectionsAssignmentReleaseAuditRepository.saveAndFlush(CollectionsAssignmentReleaseAudit.create(
            "TENANT-01",
            "INV-7000",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:05:00Z"),
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-12T00:15:00Z")
        ));
        collectionsAssignmentReleaseAuditRepository.saveAndFlush(CollectionsAssignmentReleaseAudit.create(
            "TENANT-01",
            "INV-7001",
            "collector-c@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z"),
            "collector-c@arcanaerp.com",
            Instant.parse("2026-03-12T00:20:00Z")
        ));

        var page = collectionsAssignmentReleaseAuditRepository.findHistory(
            "TENANT-01",
            "INV-7000",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "releasedAt"))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(CollectionsAssignmentReleaseAudit::getReleasedBy)
            .containsExactly("collector-b@arcanaerp.com", "collector-a@arcanaerp.com");
    }
}
