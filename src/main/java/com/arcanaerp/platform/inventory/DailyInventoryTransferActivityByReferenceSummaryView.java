package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryTransferActivityByReferenceSummaryView(
    String sku,
    LocalDate businessDate,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
