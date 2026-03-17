package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteCategoryOutcomeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    CollectionsNoteCategory category,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
