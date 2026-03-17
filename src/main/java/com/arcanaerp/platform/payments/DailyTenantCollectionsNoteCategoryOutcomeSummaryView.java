package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteCategoryOutcomeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    CollectionsNoteCategory category,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
