package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeDashboardSummaryResponse(
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
