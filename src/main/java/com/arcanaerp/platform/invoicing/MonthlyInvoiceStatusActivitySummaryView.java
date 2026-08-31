package com.arcanaerp.platform.invoicing;

import java.time.YearMonth;

public record MonthlyInvoiceStatusActivitySummaryView(
    YearMonth businessMonth,
    long transitionCount,
    long invoiceCount
) {
}
