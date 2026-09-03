package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyInventoryTransferActivityByReferenceSummaryView(
    String sku,
    LocalDate businessWeekStart,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
