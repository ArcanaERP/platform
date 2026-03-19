package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsCurrentAssigneeFollowUpOutcomeSummaryView(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    CollectionsFollowUpOutcome latestFollowUpOutcome,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
