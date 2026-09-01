package com.arcanaerp.platform.invoicing;

import java.time.YearMonth;

public record MonthlyInvoiceStatusActivityByCurrentStatusSummaryView(
    YearMonth businessMonth,
    InvoiceStatus currentStatus,
    long transitionCount,
    long invoiceCount
) {
}
