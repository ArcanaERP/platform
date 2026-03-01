package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
    UUID id,
    String sku,
    BigDecimal onHandQuantity,
    Instant updatedAt
) {
}
