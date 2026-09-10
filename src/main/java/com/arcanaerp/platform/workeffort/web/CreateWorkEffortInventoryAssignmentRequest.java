package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkEffortInventoryAssignmentRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotBlank String inventoryEntryCode
) {
}
