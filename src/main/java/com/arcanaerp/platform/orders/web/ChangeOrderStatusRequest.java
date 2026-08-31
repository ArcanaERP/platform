package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequest(
    @NotNull OrderStatus status,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
