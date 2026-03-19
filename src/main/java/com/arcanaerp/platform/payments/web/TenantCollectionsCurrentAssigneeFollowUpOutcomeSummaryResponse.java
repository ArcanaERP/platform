package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsCurrentAssigneeFollowUpOutcomeSummaryResponse(
    String tenantCode,
    String currencyCode,
    String assignedTo,
    String latestFollowUpOutcome,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
