package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
    String sku,
    LocalDate businessDate,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
