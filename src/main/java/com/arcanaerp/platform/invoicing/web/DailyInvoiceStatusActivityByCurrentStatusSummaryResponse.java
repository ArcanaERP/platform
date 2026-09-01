package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.LocalDate;

public record DailyInvoiceStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessDate,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
