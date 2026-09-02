package com.arcanaerp.platform.workeffort;

import java.time.YearMonth;

public record MonthlyWorkEffortStatusActivitySummaryView(
    String tenantCode,
    YearMonth businessMonth,
    long transitionCount,
    long workEffortCount
) {
}
