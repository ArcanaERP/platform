package com.arcanaerp.platform.invoicing.web;

import java.time.LocalDate;

public record WeeklyInvoiceStatusActivitySummaryResponse(
    LocalDate businessWeekStart,
    long transitionCount,
    long invoiceCount
) {
}
