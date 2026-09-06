package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record ReleaseInventoryProductInstanceAssignmentRequest(
    @NotBlank String reason,
    @NotBlank String releasedBy
) {
}
