package com.arcanaerp.platform.payments.web;

import java.time.YearMonth;

public record MonthlyTenantCollectionsAssignmentSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    long assignmentCount,
    long invoiceCount
) {
}
