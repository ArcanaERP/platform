package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeOperationsSummaryResponse(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    long currentAssignedInvoiceCount,
    BigDecimal currentOutstandingAmount,
    Instant oldestDueAt,
    long claimCount,
    long releaseCount,
    long netIntakeCount
) {
}
