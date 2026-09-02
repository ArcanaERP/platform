package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivityByLocationSummaryView(
    String sku,
    LocalDate businessWeekStart,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
