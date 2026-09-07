package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record EndInventoryItemLocationAssignmentRequest(
    @NotBlank String validThru,
    @NotBlank String reason,
    @NotBlank String endedBy
) {
}
