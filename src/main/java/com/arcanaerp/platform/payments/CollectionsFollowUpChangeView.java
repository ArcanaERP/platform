package com.arcanaerp.platform.payments;

import java.time.Instant;
import java.util.UUID;

public record CollectionsFollowUpChangeView(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    Instant previousFollowUpAt,
    Instant followUpAt,
    String changedBy,
    Instant changedAt
) {
}
