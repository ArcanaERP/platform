package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.YearMonth;

public record MonthlyWorkEffortStatusActivityByCurrentStatusSummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
