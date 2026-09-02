package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivityByLocationSummaryResponse(
    String sku,
    YearMonth businessMonth,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
