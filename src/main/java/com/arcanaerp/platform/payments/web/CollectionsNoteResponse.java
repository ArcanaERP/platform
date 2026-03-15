package com.arcanaerp.platform.payments.web;

import java.time.Instant;
import java.util.UUID;

public record CollectionsNoteResponse(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String note,
    String notedBy,
    String category,
    String outcome,
    Instant notedAt
) {
}
