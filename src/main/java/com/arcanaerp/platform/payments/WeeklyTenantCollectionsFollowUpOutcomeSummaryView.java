package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsFollowUpOutcomeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
