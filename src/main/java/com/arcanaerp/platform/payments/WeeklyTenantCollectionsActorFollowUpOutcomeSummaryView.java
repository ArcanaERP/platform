package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsActorFollowUpOutcomeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    String changedBy,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
