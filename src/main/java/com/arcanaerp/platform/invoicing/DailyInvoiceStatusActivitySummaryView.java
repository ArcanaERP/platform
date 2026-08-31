package com.arcanaerp.platform.invoicing;

import java.time.LocalDate;

public record DailyInvoiceStatusActivitySummaryView(
    LocalDate businessDate,
    long transitionCount,
    long invoiceCount
) {
}
