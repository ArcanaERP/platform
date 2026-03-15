package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void filtersNotesByActorAndRangeNewestFirst() {
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "First note",
            "collector-a@arcanaerp.com",
            Instant.parse("2026-03-14T12:00:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "Second note",
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-14T12:05:00Z")
        ));
        collectionsNoteRepository.saveAndFlush(CollectionsNote.create(
            "tenant-a",
            "inv-1001",
            "Other invoice",
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-14T12:10:00Z")
        ));

        var page = collectionsNoteRepository.findHistoryFiltered(
            "TENANT-A",
            "INV-1000",
            "collector-b@arcanaerp.com",
            Instant.parse("2026-03-14T12:04:00Z"),
            Instant.parse("2026-03-14T12:06:00Z"),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getNote()).isEqualTo("Second note");
        assertThat(page.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-1000");
    }
}
