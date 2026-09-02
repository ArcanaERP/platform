package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryAdjustmentActivityByLocationSummaryResponse(
    String sku,
    LocalDate businessWeekStart,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
