package com.arcanaerp.platform.workeffort.web;

import java.time.YearMonth;

public record MonthlyWorkEffortAssignmentActivityByAssigneeSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
