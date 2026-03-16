package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsNoteOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String outcome,
    long noteCount,
    long invoiceCount
) {
}
