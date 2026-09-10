package com.arcanaerp.platform.workeffort;

public record RegisterOrderRequirementCommitmentCommand(
    Long orderLineItemId,
    Long requirementId,
    String description,
    Integer quantity
) {
}
