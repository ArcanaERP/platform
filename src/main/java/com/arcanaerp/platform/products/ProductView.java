package com.arcanaerp.platform.products;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
    UUID id,
    String sku,
    String name,
    boolean active,
    UUID categoryId,
    String categoryCode,
    String categoryName,
    BigDecimal currentPrice,
    String currencyCode,
    Instant pricedAt
) {
}
