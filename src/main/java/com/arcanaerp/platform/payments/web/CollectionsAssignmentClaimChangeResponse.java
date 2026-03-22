package com.arcanaerp.platform.payments.web;

import java.time.Instant;
import java.util.UUID;

public record CollectionsAssignmentClaimChangeResponse(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String claimedBy,
    Instant claimedAt
) {
}
