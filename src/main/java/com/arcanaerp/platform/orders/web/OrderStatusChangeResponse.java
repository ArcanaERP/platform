package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangeResponse(
    UUID id,
    String orderNumber,
    OrderStatus previousStatus,
    OrderStatus currentStatus,
    Instant changedAt
) {
}
