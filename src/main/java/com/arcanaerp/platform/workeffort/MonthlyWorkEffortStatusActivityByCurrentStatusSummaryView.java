package com.arcanaerp.platform.workeffort;

import java.time.YearMonth;

public record MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView(
    String tenantCode,
    YearMonth businessMonth,
    WorkEffortStatus currentStatus,
    long transitionCount,
    long workEffortCount
) {
}
