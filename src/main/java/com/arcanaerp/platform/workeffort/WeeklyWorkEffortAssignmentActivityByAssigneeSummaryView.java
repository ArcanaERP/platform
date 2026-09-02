package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
