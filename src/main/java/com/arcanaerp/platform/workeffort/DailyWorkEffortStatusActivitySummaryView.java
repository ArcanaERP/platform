package com.arcanaerp.platform.workeffort;

import java.time.LocalDate;

public record DailyWorkEffortStatusActivitySummaryView(
    String tenantCode,
    LocalDate businessDate,
    long transitionCount,
    long workEffortCount
) {
}
