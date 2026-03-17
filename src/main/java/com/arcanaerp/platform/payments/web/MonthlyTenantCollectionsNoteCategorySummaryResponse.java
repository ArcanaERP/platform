package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteCategorySummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String category,
    long noteCount,
    long invoiceCount
) {
}
