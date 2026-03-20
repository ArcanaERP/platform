package com.arcanaerp.platform.payments.web;

import com.arcanaerp.platform.payments.ReceivablesAgingBucket;
import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsAssigneeAgingSummaryResponse(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    ReceivablesAgingBucket agingBucket,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
