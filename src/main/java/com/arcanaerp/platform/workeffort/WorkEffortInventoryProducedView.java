package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortInventoryProducedView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long inventoryEntryId,
    Instant createdAt
) {
}
