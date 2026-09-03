package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivityByAdjustedBySummaryResponse(
    String sku,
    YearMonth businessMonth,
    String adjustedBy,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
