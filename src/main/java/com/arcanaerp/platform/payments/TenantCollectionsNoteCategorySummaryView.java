package com.arcanaerp.platform.payments;

public record TenantCollectionsNoteCategorySummaryView(
    String tenantCode,
    CollectionsNoteCategory category,
    long noteCount,
    long invoiceCount
) {
}
