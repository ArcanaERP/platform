package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryTransferActivitySummaryResponse(
    String sku,
    LocalDate businessDate,
    String sourceLocationCode,
    String destinationLocationCode,
    String adjustedBy,
    long transferCount,
    BigDecimal totalQuantity
) {
}
