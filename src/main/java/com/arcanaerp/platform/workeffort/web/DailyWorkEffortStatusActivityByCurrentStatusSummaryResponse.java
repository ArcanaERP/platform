package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.LocalDate;

public record DailyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
