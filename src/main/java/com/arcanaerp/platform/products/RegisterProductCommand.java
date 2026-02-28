package com.arcanaerp.platform.products;

import java.math.BigDecimal;

public record RegisterProductCommand(
    String sku,
    String name,
    String categoryCode,
    String categoryName,
    BigDecimal amount,
    String currencyCode
) {
}
