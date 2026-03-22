package com.arcanaerp.platform.payments;

import java.time.Instant;
import java.util.UUID;

public record CollectionsAssignmentClaimChangeView(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String claimedBy,
    Instant claimedAt
) {
}
