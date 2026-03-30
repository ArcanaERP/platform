package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsAssigneeDashboardSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String assignedTo,
    long completionCount,
    long invoiceCount,
    long contactedInvoiceCount,
    long leftVoicemailInvoiceCount,
    long promiseToPayInvoiceCount,
    long noResponseInvoiceCount
) {
}
