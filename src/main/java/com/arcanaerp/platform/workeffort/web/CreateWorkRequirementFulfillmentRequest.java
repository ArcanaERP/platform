package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkRequirementFulfillmentRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotNull Long requirementId,
    String description
) {
}
