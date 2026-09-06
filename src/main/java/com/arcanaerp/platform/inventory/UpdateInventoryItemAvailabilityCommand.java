package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record UpdateInventoryItemAvailabilityCommand(
    String sku,
    String locationCode,
    BigDecimal availableQuantityDelta,
    BigDecimal soldQuantityDelta,
    String reason,
    String changedBy
) {
}
