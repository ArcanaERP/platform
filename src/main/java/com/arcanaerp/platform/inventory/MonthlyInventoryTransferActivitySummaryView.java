package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryTransferActivitySummaryView(
    String sku,
    YearMonth businessMonth,
    String sourceLocationCode,
    String destinationLocationCode,
    String adjustedBy,
    long transferCount,
    BigDecimal totalQuantity
) {
}
