package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsNoteCategorySummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String category,
    long noteCount,
    long invoiceCount
) {
}
