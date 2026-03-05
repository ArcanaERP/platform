package com.arcanaerp.platform.products;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceView(
    UUID id,
    String sku,
    BigDecimal amount,
    String currencyCode,
    Instant effectiveFrom
) {
}
