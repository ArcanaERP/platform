package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryPickupDropoffActivitySummaryResponse(
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
