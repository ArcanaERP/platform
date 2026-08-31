package com.arcanaerp.platform.workeffort;

import java.time.YearMonth;

public record MonthlyWorkEffortAssignmentActivitySummaryView(
    String tenantCode,
    YearMonth businessMonth,
    long assignmentCount,
    long workEffortCount
) {
}
