package com.arcanaerp.platform.orders.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateOrderLineRequest(
    @NotBlank String productSku,
    @NotNull @DecimalMin("0.0001") BigDecimal quantity,
    @NotNull @DecimalMin("0.0001") BigDecimal unitPrice
) {
}
