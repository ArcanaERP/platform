package com.arcanaerp.platform.payments.web;

import java.time.LocalDate;

public record DailyTenantCollectionsAssignmentSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    long assignmentCount,
    long invoiceCount
) {
}
