package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryTransferActivityByReferenceSummaryView(
    String sku,
    YearMonth businessMonth,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
