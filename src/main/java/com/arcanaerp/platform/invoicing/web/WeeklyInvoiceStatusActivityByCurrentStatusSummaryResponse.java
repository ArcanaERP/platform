package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.LocalDate;

public record WeeklyInvoiceStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessWeekStart,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
