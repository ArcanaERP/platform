package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsReleaseSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    String releasedBy,
    long releaseCount,
    long invoiceCount
) {
}
