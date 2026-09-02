package com.arcanaerp.platform.communicationevents;

import java.time.LocalDate;

public record WeeklyCommunicationEventStatusActivitySummaryView(
    LocalDate businessWeekStart,
    long transitionCount,
    long eventCount
) {
}
