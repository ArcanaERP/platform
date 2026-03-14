package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssignmentSummaryView(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    long assignedInvoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
