package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeOperationsSummaryView(
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
