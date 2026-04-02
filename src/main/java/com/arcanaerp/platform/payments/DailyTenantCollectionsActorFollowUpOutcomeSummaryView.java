package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsActorFollowUpOutcomeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    String changedBy,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
