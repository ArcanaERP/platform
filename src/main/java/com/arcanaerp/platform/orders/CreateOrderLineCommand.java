package com.arcanaerp.platform.orders;

import java.math.BigDecimal;

public record CreateOrderLineCommand(
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice
) {
}
