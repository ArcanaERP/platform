package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryTransferActivityByReferenceSummaryResponse(
    String sku,
    YearMonth businessMonth,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
