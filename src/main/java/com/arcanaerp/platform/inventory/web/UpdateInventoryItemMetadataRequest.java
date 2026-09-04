package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryItemMetadataRequest(
    @NotBlank String unitOfMeasurementCode,
    @NotBlank String classificationCode
) {
}
