package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryLocationTypeRequest(
    @NotBlank String code,
    @NotBlank String description
) {
}
