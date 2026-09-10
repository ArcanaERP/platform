package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortInventoryAssignmentResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String inventoryEntryCode,
    Instant createdAt
) {
}
