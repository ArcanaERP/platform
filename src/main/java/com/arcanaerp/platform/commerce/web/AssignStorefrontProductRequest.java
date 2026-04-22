package com.arcanaerp.platform.commerce.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AssignStorefrontProductRequest(
    @NotBlank String tenantCode,
    @NotBlank String sku,
    String merchandisingName,
    @Min(0) int position,
    boolean active
) {
}
