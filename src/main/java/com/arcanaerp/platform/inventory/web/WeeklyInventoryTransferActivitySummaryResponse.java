package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryTransferActivitySummaryResponse(
    String sku,
    LocalDate businessWeekStart,
    String sourceLocationCode,
    String destinationLocationCode,
    String adjustedBy,
    long transferCount,
    BigDecimal totalQuantity
) {
}
