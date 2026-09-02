package com.arcanaerp.platform.communicationevents;

import java.time.LocalDate;

public record WeeklyCommunicationEventStatusActivityByCurrentStatusSummaryView(
    LocalDate businessWeekStart,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
