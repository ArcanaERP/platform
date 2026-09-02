package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivitySummaryResponse(
    String sku,
    String locationCode,
    LocalDate businessWeekStart,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
