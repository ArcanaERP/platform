package com.arcanaerp.platform.payments;

public record TenantCollectionsNoteOutcomeSummaryView(
    String tenantCode,
    CollectionsNoteOutcome outcome,
    long noteCount,
    long invoiceCount
) {
}
