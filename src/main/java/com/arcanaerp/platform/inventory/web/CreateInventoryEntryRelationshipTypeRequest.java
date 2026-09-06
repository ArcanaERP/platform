package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryEntryRelationshipTypeRequest(
    @NotBlank String code,
    @NotBlank String description,
    String comments
) {
}
