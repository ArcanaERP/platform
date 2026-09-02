package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivitySummaryResponse(
    String sku,
    String locationCode,
    LocalDate businessDate,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
