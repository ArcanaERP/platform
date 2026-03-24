package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsReleaseSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
