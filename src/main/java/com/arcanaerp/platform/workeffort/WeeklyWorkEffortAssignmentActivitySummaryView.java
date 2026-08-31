package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record WeeklyWorkEffortAssignmentActivitySummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    long assignmentCount,
    long workEffortCount
) {
}
