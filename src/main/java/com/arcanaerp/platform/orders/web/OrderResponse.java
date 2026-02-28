package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.orders.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    String customerEmail,
    OrderStatus status,
    String currencyCode,
    BigDecimal totalAmount,
    Instant createdAt,
    List<OrderLineResponse> lines
) {
}
