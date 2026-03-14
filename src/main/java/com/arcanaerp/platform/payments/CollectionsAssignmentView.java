package com.arcanaerp.platform.payments;

import java.time.Instant;

public record CollectionsAssignmentView(
    String tenantCode,
    String invoiceNumber,
    String assignedTo,
    String assignedBy,
    Instant assignedAt
) {
}
