package com.arcanaerp.platform.payments.web;

public record TenantCollectionsNoteOutcomeSummaryResponse(
    String tenantCode,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
