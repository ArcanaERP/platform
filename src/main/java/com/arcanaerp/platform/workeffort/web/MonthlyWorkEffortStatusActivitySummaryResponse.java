package com.arcanaerp.platform.workeffort.web;

import java.time.YearMonth;

public record MonthlyWorkEffortStatusActivitySummaryResponse(
    String tenantCode,
    YearMonth businessMonth,
    long transitionCount,
    long workEffortCount
) {
}
