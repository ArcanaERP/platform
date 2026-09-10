package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortInventoryAssignmentCommand(
    String tenantCode,
    String effortNumber,
    String inventoryEntryCode
) {
}
