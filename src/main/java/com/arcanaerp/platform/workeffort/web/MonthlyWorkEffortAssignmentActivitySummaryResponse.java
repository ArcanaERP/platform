package com.arcanaerp.platform.workeffort.web;

import java.time.YearMonth;

public record MonthlyWorkEffortAssignmentActivitySummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    long assignmentCount,
    long workEffortCount
) {
}
