package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryTransferActivitySummaryResponse(
    String sku,
    YearMonth businessMonth,
    String sourceLocationCode,
    String destinationLocationCode,
    String adjustedBy,
    long transferCount,
    BigDecimal totalQuantity
) {
}
