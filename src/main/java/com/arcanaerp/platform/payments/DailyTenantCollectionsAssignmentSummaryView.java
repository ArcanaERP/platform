package com.arcanaerp.platform.payments;

import java.time.LocalDate;

public record DailyTenantCollectionsAssignmentSummaryView(
    String tenantCode,
    LocalDate businessDate,
    long assignmentCount,
    long invoiceCount
) {
}
