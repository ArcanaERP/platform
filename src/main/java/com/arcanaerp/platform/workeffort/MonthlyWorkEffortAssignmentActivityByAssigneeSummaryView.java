package com.arcanaerp.platform.workeffort;

import java.time.YearMonth;

public record MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
