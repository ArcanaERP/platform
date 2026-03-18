package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsFollowUpOutcomeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
