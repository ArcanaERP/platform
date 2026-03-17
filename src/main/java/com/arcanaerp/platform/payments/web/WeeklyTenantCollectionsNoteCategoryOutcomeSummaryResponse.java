package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteCategoryOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String category,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
