package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record WeeklyWorkEffortAssignmentActivityByAssigneeSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
