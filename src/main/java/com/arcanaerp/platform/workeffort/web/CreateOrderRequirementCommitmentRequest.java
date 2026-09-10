package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequirementCommitmentRequest(
    @NotNull Long orderLineItemId,
    @NotNull Long requirementId,
    String description,
    Integer quantity
) {
}
