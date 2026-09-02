package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.LocalDate;

public record WeeklyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
    String tenantCode,
    LocalDate businessWeekStart,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
