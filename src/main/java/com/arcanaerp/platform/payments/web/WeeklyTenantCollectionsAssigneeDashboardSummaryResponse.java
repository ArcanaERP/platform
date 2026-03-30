package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsAssigneeDashboardSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String assignedTo,
    long completionCount,
    long invoiceCount,
    long contactedInvoiceCount,
    long leftVoicemailInvoiceCount,
    long promiseToPayInvoiceCount,
    long noResponseInvoiceCount
) {
}
