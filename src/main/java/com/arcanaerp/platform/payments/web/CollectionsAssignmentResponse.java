package com.arcanaerp.platform.payments.web;

import java.time.Instant;

public record CollectionsAssignmentResponse(
    String tenantCode,
    String invoiceNumber,
    String assignedTo,
    String assignedBy,
    Instant assignedAt,
    Instant followUpAt,
    String followUpSetBy,
    Instant followUpSetAt
) {
}
