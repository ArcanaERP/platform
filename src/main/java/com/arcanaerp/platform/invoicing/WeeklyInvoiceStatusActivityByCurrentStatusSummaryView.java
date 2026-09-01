package com.arcanaerp.platform.invoicing;

import java.time.LocalDate;

public record WeeklyInvoiceStatusActivityByCurrentStatusSummaryView(
    LocalDate businessWeekStart,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
