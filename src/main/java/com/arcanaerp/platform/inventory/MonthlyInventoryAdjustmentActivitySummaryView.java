package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivitySummaryView(
    String sku,
    String locationCode,
    YearMonth businessMonth,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
