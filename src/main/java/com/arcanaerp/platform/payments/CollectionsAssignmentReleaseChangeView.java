package com.arcanaerp.platform.payments;

import java.time.Instant;
import java.util.UUID;

public record CollectionsAssignmentReleaseChangeView(
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
