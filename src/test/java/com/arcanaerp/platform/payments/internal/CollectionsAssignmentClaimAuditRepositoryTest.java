package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CollectionsAssignmentClaimAuditRepositoryTest {

    @Autowired
    private CollectionsAssignmentClaimAuditRepository collectionsAssignmentClaimAuditRepository;

    @Test
    void listsClaimHistoryNewestFirstForInvoice() {
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-01",
            "INV-7200",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-01",
            "INV-7200",
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-01",
            "INV-7201",
            "collector-c@arcanaerp.com",
            Instant.parse("2026-03-12T00:20:00Z")
        ));

        var page = collectionsAssignmentClaimAuditRepository.findHistory(
            "TENANT-01",
            "INV-7200",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "claimedAt"))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(CollectionsAssignmentClaimAudit::getClaimedBy)
            .containsExactly("collector-b@arcanaerp.com", "collector-a@arcanaerp.com");
    }

    @Test
    void filtersTenantClaimHistoryByInvoiceActorAndClaimedAtRange() {
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-02",
            "INV-7300",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-02",
            "INV-7301",
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));
        collectionsAssignmentClaimAuditRepository.saveAndFlush(CollectionsAssignmentClaimAudit.create(
            "TENANT-02",
            "INV-7300",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:20:00Z")
        ));

        var page = collectionsAssignmentClaimAuditRepository.findTenantHistoryFiltered(
            "TENANT-02",
            "INV-7300",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-12T00:11:00Z"),
            Instant.parse("2026-03-12T00:20:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "claimedAt"))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getInvoiceNumber()).isEqualTo("INV-7300");
        assertThat(page.getContent().getFirst().getClaimedBy()).isEqualTo("collector-a@arcanaerp.com");
        assertThat(page.getContent().getFirst().getClaimedAt()).isEqualTo(Instant.parse("2026-03-12T00:20:00Z"));
    }
}
