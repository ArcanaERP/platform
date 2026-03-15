package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.payments.CollectionsNoteCategory;
import com.arcanaerp.platform.payments.CollectionsNoteOutcome;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class CollectionsNoteRepositoryTest {

    @Autowired
    private CollectionsNoteRepository collectionsNoteRepository;

    @Autowired
    private CollectionsAssignmentRepository collectionsAssignmentRepository;

    @Test
    void filtersNotesByActorAndRangeNewestFirst() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "First note",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.CONTACT_ATTEMPT,
            CollectionsNoteOutcome.AWAITING_RESPONSE,
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "Second note",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.ESCALATION,
            CollectionsNoteOutcome.ESCALATED,
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Other invoice",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.INTERNAL_UPDATE,
            CollectionsNoteOutcome.RESOLVED,
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        var page = collectionsNoteRepository.findHistoryFiltered(
            "TENANT-A",
            "INV-1000",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.ESCALATION,
            CollectionsNoteOutcome.ESCALATED,
            Instant.parse("2026-03-14T12:04:00Z"),
            Instant.parse("2026-03-14T12:06:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getNote()).isEqualTo("Second note");
        assertThat(page.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-1000");
    }

    @Test
    void filtersTenantNotesByInvoiceActorAndRange() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "First invoice note",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.CONTACT_ATTEMPT,
            CollectionsNoteOutcome.NO_CONTACT,
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Second invoice note",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.DISPUTE,
            CollectionsNoteOutcome.DISPUTE_OPENED,
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-b",
            "inv-1002",
            "Other tenant",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.INTERNAL_UPDATE,
            CollectionsNoteOutcome.RESOLVED,
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        var page = collectionsNoteRepository.findTenantHistoryFiltered(
            "TENANT-A",
            "INV-1001",
            null,
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.DISPUTE,
            CollectionsNoteOutcome.DISPUTE_OPENED,
            Instant.parse("2026-03-14T12:04:00Z"),
            Instant.parse("2026-03-14T12:06:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-1001");
        assertThat(page.getContent().get(0).getNotedBy()).isEqualTo("collector-b@arcanaerp.com");
    }

    @Test
    void listsTenantNotesForOutcomeSummaryFilters() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "Promise noted",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.PAYMENT_PROMISE,
            CollectionsNoteOutcome.PROMISE_TO_PAY,
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Dispute logged",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.DISPUTE,
            CollectionsNoteOutcome.DISPUTE_OPENED,
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1002",
            "Escalated",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.ESCALATION,
            CollectionsNoteOutcome.ESCALATED,
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        var notes = collectionsNoteRepository.findTenantHistoryForOutcomeSummary(
            "TENANT-A",
            "collector-a@arcanaerp.com",
            null,
            Instant.parse("2026-03-14T11:59:00Z"),
            Instant.parse("2026-03-14T12:06:00Z")
        );

        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(CollectionsNote::getOutcome)
            .containsExactly(CollectionsNoteOutcome.DISPUTE_OPENED, CollectionsNoteOutcome.PROMISE_TO_PAY);
    }

    @Test
    void listsTenantNotesForCategorySummaryFilters() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "Promise noted",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.PAYMENT_PROMISE,
            CollectionsNoteOutcome.PROMISE_TO_PAY,
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Another promise",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.PAYMENT_PROMISE,
            CollectionsNoteOutcome.PROMISE_TO_PAY,
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1002",
            "Escalated",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.ESCALATION,
            CollectionsNoteOutcome.ESCALATED,
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        var notes = collectionsNoteRepository.findTenantHistoryForCategorySummary(
            "TENANT-A",
            "collector-a@arcanaerp.com",
            CollectionsNoteOutcome.PROMISE_TO_PAY,
            Instant.parse("2026-03-14T11:59:00Z"),
            Instant.parse("2026-03-14T12:06:00Z")
        );

        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(CollectionsNote::getCategory)
            .containsExactly(CollectionsNoteCategory.PAYMENT_PROMISE, CollectionsNoteCategory.PAYMENT_PROMISE);
    }

    @Test
    void listsTenantNotesFilteredByCurrentAssignedTo() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "Promise noted",
            "collector-a@arcanaerp.com",
            CollectionsNoteCategory.PAYMENT_PROMISE,
            CollectionsNoteOutcome.PROMISE_TO_PAY,
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Dispute opened",
            "collector-b@arcanaerp.com",
            CollectionsNoteCategory.DISPUTE,
            CollectionsNoteOutcome.DISPUTE_OPENED,
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1002",
            "No assignment",
            "collector-c@arcanaerp.com",
            CollectionsNoteCategory.CONTACT_ATTEMPT,
            CollectionsNoteOutcome.NO_CONTACT,
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "tenant-a",
            "inv-1000",
            "collector-a@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-14T11:55:00Z")
        ));
        collectionsAssignmentRepository.saveAndFlush(CollectionsAssignment.create(
            "tenant-a",
            "inv-1001",
            "collector-b@arcanaerp.com",
            "manager@arcanaerp.com",
            Instant.parse("2026-03-14T11:56:00Z")
        ));

        var notes = collectionsNoteRepository.findTenantHistoryFiltered(
            "TENANT-A",
            null,
            "collector-b@arcanaerp.com",
            null,
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(notes.getContent()).hasSize(1);
        assertThat(notes.getContent()).extracting(CollectionsNote::getInvoiceNumber)
            .containsExactly("INV-1001");
    }
}
