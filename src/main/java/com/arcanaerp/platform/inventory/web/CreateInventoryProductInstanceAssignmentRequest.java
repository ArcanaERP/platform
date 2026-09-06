package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryProductInstanceAssignmentRequest(
    @NotBlank String sku,
    @NotBlank String locationCode,
    @NotBlank String productInstanceCode,
    @NotBlank String assignedBy
) {
}
