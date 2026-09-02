package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
