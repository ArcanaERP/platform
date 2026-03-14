package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record WeeklyTenantCollectionsAssignmentSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    long assignmentCount,
    long invoiceCount
) {
}
