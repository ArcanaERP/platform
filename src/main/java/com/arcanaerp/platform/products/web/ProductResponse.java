package com.arcanaerp.platform.products.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String sku,
    String name,
    UUID categoryId,
    String categoryCode,
    String categoryName,
    BigDecimal currentPrice,
    String currencyCode,
    Instant pricedAt
) {
}
