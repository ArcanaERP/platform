package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteOutcomeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
