package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemAvailabilityChangeView(
    UUID id,
    String sku,
    String locationCode,
    BigDecimal previousAvailableQuantity,
    BigDecimal currentAvailableQuantity,
    BigDecimal availableQuantityDelta,
    BigDecimal previousSoldQuantity,
    BigDecimal currentSoldQuantity,
    BigDecimal soldQuantityDelta,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
