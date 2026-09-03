package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivityByAdjustedBySummaryView(
    String sku,
    YearMonth businessMonth,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
