package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
    String sku,
    LocalDate businessWeekStart,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
