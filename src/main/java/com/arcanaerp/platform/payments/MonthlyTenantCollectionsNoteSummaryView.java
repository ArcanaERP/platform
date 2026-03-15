package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    long noteCount,
    long invoiceCount
) {
}
