package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsFollowUpOutcomeSummaryResponse(
    String tenantCode,
    String currencyCode,
    String latestFollowUpOutcome,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
