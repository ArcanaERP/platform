package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateInventoryItemRequest(
    @NotBlank String sku,
    @NotBlank String locationCode,
    @NotNull @PositiveOrZero BigDecimal onHandQuantity,
    String unitOfMeasurementCode,
    String classificationCode
) {
}
