package com.arcanaerp.platform.communicationevents;

import java.time.YearMonth;

public record MonthlyCommunicationEventStatusActivitySummaryView(
    YearMonth businessMonth,
    long transitionCount,
    long eventCount
) {
}
