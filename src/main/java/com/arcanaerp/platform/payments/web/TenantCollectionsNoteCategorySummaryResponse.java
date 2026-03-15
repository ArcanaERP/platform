package com.arcanaerp.platform.payments.web;

public record TenantCollectionsNoteCategorySummaryResponse(
    String tenantCode,
    String category,
    long noteCount,
    long invoiceCount
) {
}
