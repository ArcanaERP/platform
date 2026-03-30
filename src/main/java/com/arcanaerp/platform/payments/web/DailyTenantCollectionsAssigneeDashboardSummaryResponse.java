package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsAssigneeDashboardSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String assignedTo,
    long completionCount,
    long invoiceCount,
    long contactedInvoiceCount,
    long leftVoicemailInvoiceCount,
    long promiseToPayInvoiceCount,
    long noResponseInvoiceCount
) {
}
