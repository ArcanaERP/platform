package com.arcanaerp.platform.invoicing;

import java.time.LocalDate;

public record DailyInvoiceStatusActivityByCurrentStatusSummaryView(
    LocalDate businessDate,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
