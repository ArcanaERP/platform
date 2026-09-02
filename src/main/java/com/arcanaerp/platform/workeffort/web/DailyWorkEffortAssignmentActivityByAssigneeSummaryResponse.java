package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record DailyWorkEffortAssignmentActivityByAssigneeSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    String assignedTo,
    long assignmentCount,
    long workEffortCount
) {
}
