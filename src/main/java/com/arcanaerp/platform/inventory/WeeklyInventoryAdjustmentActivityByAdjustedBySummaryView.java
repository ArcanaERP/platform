package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivityByAdjustedBySummaryView(
    String sku,
    LocalDate businessWeekStart,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
