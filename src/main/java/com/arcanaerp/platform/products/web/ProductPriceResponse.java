package com.arcanaerp.platform.products.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceResponse(
    UUID id,
    String sku,
    BigDecimal amount,
    String currencyCode,
    Instant effectiveFrom
) {
}
