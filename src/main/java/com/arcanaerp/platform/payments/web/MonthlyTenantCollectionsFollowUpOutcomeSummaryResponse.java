package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsFollowUpOutcomeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
