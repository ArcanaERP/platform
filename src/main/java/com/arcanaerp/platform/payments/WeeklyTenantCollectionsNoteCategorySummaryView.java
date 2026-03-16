package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteCategorySummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    CollectionsNoteCategory category,
    long noteCount,
    long invoiceCount
) {
}
