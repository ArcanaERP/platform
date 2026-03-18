package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantCollectionsFollowUpOutcomeSummaryView(
    String tenantCode,
    String currencyCode,
    CollectionsFollowUpOutcome latestFollowUpOutcome,
    long invoiceCount,
    BigDecimal totalOutstandingAmount,
    Instant oldestDueAt
) {
}
