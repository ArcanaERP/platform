package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeDashboardSummaryView(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    long assignedInvoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt,
    long noRecordedOutcomeInvoiceCount,
    long contactedInvoiceCount,
    long leftVoicemailInvoiceCount,
    long promiseToPayInvoiceCount,
    long noResponseInvoiceCount
) {
}
