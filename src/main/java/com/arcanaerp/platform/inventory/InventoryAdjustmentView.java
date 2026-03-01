package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryAdjustmentView(
    UUID id,
    String sku,
    BigDecimal previousOnHandQuantity,
    BigDecimal quantityDelta,
    BigDecimal currentOnHandQuantity,
    String reason,
    String adjustedBy,
    Instant adjustedAt
) {
}
