package com.arcanaerp.platform.communicationevents;

import java.time.LocalDate;

public record DailyCommunicationEventStatusActivityByCurrentStatusSummaryView(
    LocalDate businessDate,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
