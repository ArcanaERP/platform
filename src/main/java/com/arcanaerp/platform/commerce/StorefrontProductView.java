package com.arcanaerp.platform.commerce;

import com.arcanaerp.platform.products.ProductOrderability;
import java.time.Instant;
import java.util.UUID;

public record StorefrontProductView(
    UUID id,
    String tenantCode,
    String storefrontCode,
    String sku,
    String merchandisingName,
    int position,
    boolean active,
    ProductOrderability currentOrderability,
    Instant createdAt
) {
}
