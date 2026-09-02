package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryAdjustmentActivityByLocationSummaryView(
    String sku,
    YearMonth businessMonth,
    String locationCode,
    long adjustmentCount,
    BigDecimal netQuantityDelta
) {
}
