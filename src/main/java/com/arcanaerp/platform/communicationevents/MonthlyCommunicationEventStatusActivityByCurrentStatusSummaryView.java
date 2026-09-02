package com.arcanaerp.platform.communicationevents;

import java.time.YearMonth;

public record MonthlyCommunicationEventStatusActivityByCurrentStatusSummaryView(
    YearMonth businessMonth,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
