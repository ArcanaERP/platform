package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsFollowUpOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
