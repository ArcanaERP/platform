package com.arcanaerp.platform.payments;

import java.time.Instant;
import java.util.UUID;

public record CollectionsFollowUpChangeView(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    Instant previousFollowUpAt,
    Instant followUpAt,
    CollectionsFollowUpOutcome outcome,
    String changedBy,
    Instant changedAt
) {
}
