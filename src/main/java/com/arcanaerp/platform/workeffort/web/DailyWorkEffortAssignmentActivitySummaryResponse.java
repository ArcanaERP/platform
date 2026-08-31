package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record DailyWorkEffortAssignmentActivitySummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    long assignmentCount,
    long workEffortCount
) {
}
