package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortInventoryProducedCommand(
    String tenantCode,
    String effortNumber,
    Long inventoryEntryId
) {
}
