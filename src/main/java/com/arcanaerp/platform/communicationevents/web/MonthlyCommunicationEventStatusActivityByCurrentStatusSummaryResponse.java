package com.arcanaerp.platform.communicationevents.web;

import java.time.YearMonth;

public record MonthlyCommunicationEventStatusActivityByCurrentStatusSummaryResponse(
    YearMonth businessMonth,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
