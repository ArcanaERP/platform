package com.arcanaerp.platform.invoicing.web;

import java.time.YearMonth;

public record MonthlyInvoiceStatusActivitySummaryResponse(
    YearMonth businessMonth,
    long transitionCount,
    long invoiceCount
) {
}
