package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteOutcomeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
