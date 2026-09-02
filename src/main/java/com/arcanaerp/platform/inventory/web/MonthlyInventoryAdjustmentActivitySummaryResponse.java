package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivitySummaryResponse(
    String sku,
    String locationCode,
    YearMonth businessMonth,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
