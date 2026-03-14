package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record WeeklyTenantCollectionsAssignmentSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    long assignmentCount,
    long invoiceCount
) {
}
