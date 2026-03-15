package com.arcanaerp.platform.payments;

import java.time.Instant;
import java.util.UUID;

public record CollectionsNoteView(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String note,
    String notedBy,
    CollectionsNoteCategory category,
    CollectionsNoteOutcome outcome,
    Instant notedAt
) {
}
