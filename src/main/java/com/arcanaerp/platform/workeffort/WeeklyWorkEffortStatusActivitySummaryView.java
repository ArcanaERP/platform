package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record WeeklyWorkEffortStatusActivitySummaryView(
    String tenantCode,
    LocalDate businessWeekStart,
    long transitionCount,
    long workEffortCount
) {
}
