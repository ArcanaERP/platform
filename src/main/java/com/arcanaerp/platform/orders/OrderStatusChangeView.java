package com.arcanaerp.platform.orders;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangeView(
    UUID id,
    String orderNumber,
    OrderStatus previousStatus,
    OrderStatus currentStatus,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
