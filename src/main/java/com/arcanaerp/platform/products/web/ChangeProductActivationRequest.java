package com.arcanaerp.platform.products.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeProductActivationRequest(
    @NotNull Boolean active,
    @NotBlank String reason,
    @NotBlank String tenantCode,
    @NotBlank String changedBy
) {
}
