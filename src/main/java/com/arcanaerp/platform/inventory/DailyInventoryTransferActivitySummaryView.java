package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryTransferActivitySummaryView(
    String sku,
    LocalDate businessDate,
    String sourceLocationCode,
    String destinationLocationCode,
    String adjustedBy,
    long transferCount,
    BigDecimal totalQuantity
) {
}
