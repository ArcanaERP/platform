package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record DailyWorkEffortAssignmentActivitySummaryView(
    String tenantCode,
    LocalDate businessDate,
    long assignmentCount,
    long workEffortCount
) {
}
