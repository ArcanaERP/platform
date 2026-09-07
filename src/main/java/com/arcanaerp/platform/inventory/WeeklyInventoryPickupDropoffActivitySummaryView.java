package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryPickupDropoffActivitySummaryView(
    String sku,
    LocalDate businessWeekStart,
    String locationCode,
    String transactionTypeCode,
    String handledBy,
    long transactionCount,
    BigDecimal totalPickupQuantity,
    BigDecimal totalDropoffQuantity,
    BigDecimal netQuantityDelta
) {
}
