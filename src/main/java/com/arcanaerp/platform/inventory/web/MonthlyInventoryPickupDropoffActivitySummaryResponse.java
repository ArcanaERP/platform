package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyInventoryPickupDropoffActivitySummaryResponse(
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
