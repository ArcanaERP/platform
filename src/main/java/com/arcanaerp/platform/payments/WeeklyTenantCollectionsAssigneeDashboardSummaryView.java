package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsAssigneeDashboardSummaryView(
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
