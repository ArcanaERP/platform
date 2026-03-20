package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeAgingSummaryView(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    ReceivablesAgingBucket agingBucket,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
