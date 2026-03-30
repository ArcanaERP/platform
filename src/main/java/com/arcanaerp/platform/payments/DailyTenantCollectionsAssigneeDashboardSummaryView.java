package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsAssigneeDashboardSummaryView(
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
