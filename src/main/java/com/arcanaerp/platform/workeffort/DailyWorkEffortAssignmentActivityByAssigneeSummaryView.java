package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record DailyWorkEffortAssignmentActivityByAssigneeSummaryView(
    String tenantCode,
    LocalDate businessDate,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
