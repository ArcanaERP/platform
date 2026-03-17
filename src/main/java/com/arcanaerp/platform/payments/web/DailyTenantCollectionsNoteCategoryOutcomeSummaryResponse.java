package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteCategoryOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String category,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
