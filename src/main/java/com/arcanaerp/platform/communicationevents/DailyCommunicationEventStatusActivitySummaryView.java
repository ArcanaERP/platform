package com.arcanaerp.platform.communicationevents;

import java.time.LocalDate;

public record DailyCommunicationEventStatusActivitySummaryView(
    LocalDate businessDate,
    long transitionCount,
    long eventCount
) {
}
