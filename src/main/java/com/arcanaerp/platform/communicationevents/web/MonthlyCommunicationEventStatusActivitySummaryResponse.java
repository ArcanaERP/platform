package com.arcanaerp.platform.communicationevents.web;

import java.time.YearMonth;

public record MonthlyCommunicationEventStatusActivitySummaryResponse(
    YearMonth businessMonth,
    long transitionCount,
    long eventCount
) {
}
