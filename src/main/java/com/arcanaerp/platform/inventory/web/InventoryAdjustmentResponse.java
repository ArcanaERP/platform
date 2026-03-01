package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryAdjustmentResponse(
    UUID id,
    String sku,
    String locationCode,
    BigDecimal previousOnHandQuantity,
    BigDecimal quantityDelta,
    BigDecimal currentOnHandQuantity,
    String reason,
    String adjustedBy,
    Instant adjustedAt
) {
}
