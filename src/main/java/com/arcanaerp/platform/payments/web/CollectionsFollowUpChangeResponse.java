package com.arcanaerp.platform.payments.web;

import java.time.Instant;
import java.util.UUID;

public record CollectionsFollowUpChangeResponse(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    Instant previousFollowUpAt,
    Instant followUpAt,
    String changedBy,
    Instant changedAt
) {
}
