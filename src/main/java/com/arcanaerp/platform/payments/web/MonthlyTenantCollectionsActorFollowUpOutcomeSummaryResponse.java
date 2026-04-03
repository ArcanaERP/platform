package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsActorFollowUpOutcomeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String changedBy,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
