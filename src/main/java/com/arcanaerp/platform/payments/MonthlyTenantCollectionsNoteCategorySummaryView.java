package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsNoteCategorySummaryView(
    String tenantCode,
    YearMonth businessMonth,
    CollectionsNoteCategory category,
    long noteCount,
    long invoiceCount
) {
}
