package com.arcanaerp.platform.invoicing.web;

import java.time.LocalDate;

public record DailyInvoiceStatusActivitySummaryResponse(
    LocalDate businessDate,
    long transitionCount,
    long invoiceCount
) {
}
