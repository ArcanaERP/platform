package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivityByLocationSummaryResponse(
    String sku,
    LocalDate businessDate,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
