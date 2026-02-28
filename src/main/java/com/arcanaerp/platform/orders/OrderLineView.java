package com.arcanaerp.platform.orders;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineView(
    UUID id,
    int lineNo,
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {
}
