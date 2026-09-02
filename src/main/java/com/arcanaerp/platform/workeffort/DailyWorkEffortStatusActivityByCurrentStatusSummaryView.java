package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record DailyWorkEffortStatusActivityByCurrentStatusSummaryView(
    String tenantCode,
    LocalDate businessDate,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
