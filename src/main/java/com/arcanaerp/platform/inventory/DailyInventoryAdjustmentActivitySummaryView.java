package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryAdjustmentActivitySummaryView(
    String sku,
    String locationCode,
    LocalDate businessDate,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
