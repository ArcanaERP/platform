package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivityByLocationSummaryView(
    String sku,
    LocalDate businessDate,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
