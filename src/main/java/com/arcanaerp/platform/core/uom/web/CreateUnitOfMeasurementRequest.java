package com.arcanaerp.platform.core.uom.web;

import jakarta.validation.constraints.NotBlank;

public record CreateUnitOfMeasurementRequest(
    @NotBlank String code,
    @NotBlank String description,
    String domain,
    String comments
) {
}
