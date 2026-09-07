package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryPickupDropoffActivitySummaryView(
    String sku,
    YearMonth businessMonth,
    String locationCode,
    String transactionTypeCode,
    String handledBy,
    long transactionCount,
    BigDecimal totalPickupQuantity,
    BigDecimal totalDropoffQuantity,
    BigDecimal netQuantityDelta
) {
}
