package com.arcanaerp.platform.workeffort.web;

import java.time.LocalDate;

public record DailyWorkEffortStatusActivitySummaryResponse(
    String tenantCode,
    LocalDate businessDate,
    long transitionCount,
    long workEffortCount
) {
}
