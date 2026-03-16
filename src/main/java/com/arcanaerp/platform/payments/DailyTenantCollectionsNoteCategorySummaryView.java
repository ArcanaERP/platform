package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteCategorySummaryView(
    String tenantCode,
    LocalDate businessDate,
    CollectionsNoteCategory category,
    long noteCount,
    long invoiceCount
) {
}
