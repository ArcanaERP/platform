package com.arcanaerp.platform.payments;

import java.time.YearMonth;

public record MonthlyTenantCollectionsAssignmentSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    long assignmentCount,
    long invoiceCount
) {
}
