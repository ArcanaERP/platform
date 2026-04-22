package com.arcanaerp.platform.commerce.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeStorefrontProductActivationRequest(
    @NotBlank String tenantCode,
    @NotNull Boolean active,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
