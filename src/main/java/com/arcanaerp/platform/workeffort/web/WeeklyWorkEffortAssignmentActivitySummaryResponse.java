package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record WeeklyWorkEffortAssignmentActivitySummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    long assignmentCount,
    long workEffortCount
) {
}
