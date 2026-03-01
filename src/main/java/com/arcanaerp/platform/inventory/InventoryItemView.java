package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemView(
    UUID id,
    String sku,
    String locationCode,
    BigDecimal onHandQuantity,
    Instant updatedAt
) {
}
