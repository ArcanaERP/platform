package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record AdjustInventoryCommand(
    String sku,
    String locationCode,
    BigDecimal quantityDelta,
    String reason,
    String adjustedBy
) {
}
