package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivitySummaryView(
    String sku,
    String locationCode,
    LocalDate businessWeekStart,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
