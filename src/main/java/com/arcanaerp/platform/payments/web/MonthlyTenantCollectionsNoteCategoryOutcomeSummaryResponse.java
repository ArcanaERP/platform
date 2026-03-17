package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteCategoryOutcomeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String category,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
