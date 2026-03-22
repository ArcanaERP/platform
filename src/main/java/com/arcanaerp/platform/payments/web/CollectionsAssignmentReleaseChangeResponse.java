package com.arcanaerp.platform.payments.web;

import java.time.Instant;
import java.util.UUID;

public record CollectionsAssignmentReleaseChangeResponse(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String assignedTo,
    String assignedBy,
    Instant assignedAt,
    String releasedBy,
    Instant releasedAt
) {
}
