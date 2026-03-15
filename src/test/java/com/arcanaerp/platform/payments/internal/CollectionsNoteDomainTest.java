package com.arcanaerp.platform.payments.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CollectionsNoteDomainTest {

    @Test
    void createsCollectionsNoteWithNormalizedFields() {
        Instant notedAt = Instant.parse("2026-03-14T12:00:00Z");

        CollectionsNote note = CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "  Call customer before Friday.  ",
            "Collector@ArcanaERP.com",
            notedAt
        );

        assertThat(note.getTenantCode()).isEqualTo("TENANT-A");
        assertThat(note.getInvoiceNumber()).isEqualTo("INV-1000");
        assertThat(note.getNote()).isEqualTo("Call customer before Friday.");
        assertThat(note.getNotedBy()).isEqualTo("collector@arcanaerp.com");
        assertThat(note.getNotedAt()).isEqualTo(notedAt);
    }

    @Test
    void rejectsBlankOrOversizedNote() {
        Instant notedAt = Instant.parse("2026-03-14T12:00:00Z");

        assertThatThrownBy(() -> CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "   ",
            "collector@arcanaerp.com",
            notedAt
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("note is required");

        assertThatThrownBy(() -> CollectionsNote.create(
            "tenant-a",
            "inv-1000",
            "x".repeat(1001),
            "collector@arcanaerp.com",
            notedAt
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("note must be at most 1000 characters");
    }
}
