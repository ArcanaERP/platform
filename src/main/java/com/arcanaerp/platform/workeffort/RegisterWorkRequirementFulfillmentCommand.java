package com.arcanaerp.platform.workeffort;

public record RegisterWorkRequirementFulfillmentCommand(
    String tenantCode,
    String effortNumber,
    Long requirementId,
    String description
) {
}
