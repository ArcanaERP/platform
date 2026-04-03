package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsActorFollowUpOutcomeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String changedBy,
    String outcome,
    long completionCount,
    long invoiceCount
) {
}
