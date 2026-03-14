package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssignmentSummaryResponse(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    long assignedInvoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
