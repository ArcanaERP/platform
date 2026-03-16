package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteCategorySummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String category,
    long noteCount,
    long invoiceCount
) {
}
