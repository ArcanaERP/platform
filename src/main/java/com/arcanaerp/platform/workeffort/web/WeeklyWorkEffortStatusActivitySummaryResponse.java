package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record WeeklyWorkEffortStatusActivitySummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    long transitionCount,
    long workEffortCount
) {
}
