package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortInventoryAssignmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String inventoryEntryCode,
    Instant createdAt
) {
}
