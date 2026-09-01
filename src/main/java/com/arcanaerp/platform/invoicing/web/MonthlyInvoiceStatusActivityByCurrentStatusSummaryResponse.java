package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.YearMonth;

public record MonthlyInvoiceStatusActivityByCurrentStatusSummaryResponse(
    YearMonth businessMonth,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
