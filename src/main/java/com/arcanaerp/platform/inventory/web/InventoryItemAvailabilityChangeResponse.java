package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemAvailabilityChangeResponse(
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
