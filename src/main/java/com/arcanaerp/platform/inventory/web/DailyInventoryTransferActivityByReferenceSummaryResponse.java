package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyInventoryTransferActivityByReferenceSummaryResponse(
    String sku,
    LocalDate businessDate,
    String referenceType,
    String referenceId,
    long transferCount,
    BigDecimal totalQuantity
) {
}
