package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortInventoryProducedResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long inventoryEntryId,
    Instant createdAt
) {
}
