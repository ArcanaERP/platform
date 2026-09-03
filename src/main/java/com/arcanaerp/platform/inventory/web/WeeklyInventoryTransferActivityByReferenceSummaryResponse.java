package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryTransferActivityByReferenceSummaryResponse(
    String sku,
    LocalDate businessWeekStart,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
