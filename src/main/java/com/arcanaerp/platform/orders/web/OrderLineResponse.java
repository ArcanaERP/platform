package com.arcanaerp.platform.orders.web;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
    UUID id,
    int lineNo,
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
