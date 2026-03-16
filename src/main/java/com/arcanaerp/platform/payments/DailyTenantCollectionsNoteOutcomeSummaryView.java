package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteOutcomeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
