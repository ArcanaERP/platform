package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    long noteCount,
    long invoiceCount
) {
}
