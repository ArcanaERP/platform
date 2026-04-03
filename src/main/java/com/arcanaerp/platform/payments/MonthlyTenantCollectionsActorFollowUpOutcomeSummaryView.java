package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsActorFollowUpOutcomeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    String changedBy,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
