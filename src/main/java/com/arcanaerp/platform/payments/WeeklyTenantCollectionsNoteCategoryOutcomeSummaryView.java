package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteCategoryOutcomeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    CollectionsNoteCategory category,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
