package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryEntryRelationshipRequest(
    @NotBlank String relationshipTypeCode,
    @NotBlank String fromSku,
    @NotBlank String fromLocationCode,
    @NotBlank String toSku,
    @NotBlank String toLocationCode,
    @NotBlank String fromRoleTypeCode,
    @NotBlank String toRoleTypeCode,
    @NotBlank String description,
    String statusCode
) {
}
