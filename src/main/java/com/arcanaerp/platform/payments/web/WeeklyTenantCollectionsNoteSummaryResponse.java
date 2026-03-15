package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    long noteCount,
    long invoiceCount
) {
}
