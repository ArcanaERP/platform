package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteOutcomeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
