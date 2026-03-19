package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsFollowUpOutcomeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    CollectionsFollowUpOutcome outcome,
    long completionCount,
    long invoiceCount
) {
}
