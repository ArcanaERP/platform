package com.arcanaerp.platform.invoicing;

import java.time.LocalDate;

public record WeeklyInvoiceStatusActivitySummaryView(
    LocalDate businessWeekStart,
    long transitionCount,
    long invoiceCount
) {
}
