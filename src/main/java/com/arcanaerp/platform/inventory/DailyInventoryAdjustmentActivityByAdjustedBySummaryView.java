package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivityByAdjustedBySummaryView(
    String sku,
    LocalDate businessDate,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
