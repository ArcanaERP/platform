package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CollectionsFollowUpAuditRepositoryTest {

    @Autowired
    private CollectionsFollowUpAuditRepository collectionsFollowUpAuditRepository;

    @Test
    void listsFollowUpHistoryNewestFirstForInvoice() {
        collectionsFollowUpAuditRepository.saveAndFlush(CollectionsFollowUpAudit.create(
            "TENANT-01",
            "INV-6000",
            null,
            Instant.parse("2026-03-13T00:00:00Z"),
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:00:00Z")
        ));
        collectionsFollowUpAuditRepository.saveAndFlush(CollectionsFollowUpAudit.create(
            "TENANT-01",
            "INV-6000",
            Instant.parse("2026-03-13T00:00:00Z"),
            null,
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:05:00Z")
        ));
        collectionsFollowUpAuditRepository.saveAndFlush(CollectionsFollowUpAudit.create(
            "TENANT-01",
            "INV-6001",
            null,
            Instant.parse("2026-03-15T00:00:00Z"),
            "manager@arcanaerp.com",
            Instant.parse("2026-03-12T00:10:00Z")
        ));

        var page = collectionsFollowUpAuditRepository.findHistory(
            "TENANT-01",
            "INV-6000",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(CollectionsFollowUpAudit::getFollowUpAt)
            .containsExactly(
                null,
                Instant.parse("2026-03-13T00:00:00Z")
            );
        assertThat(page.getContent().getFirst().getPreviousFollowUpAt()).isEqualTo(Instant.parse("2026-03-13T00:00:00Z"));
    }
}
