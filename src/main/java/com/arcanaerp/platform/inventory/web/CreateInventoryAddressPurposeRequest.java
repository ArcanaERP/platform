package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryAddressPurposeRequest(
    @NotBlank String code,
    @NotBlank String description
) {
}
