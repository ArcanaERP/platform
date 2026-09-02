package com.arcanaerp.platform.communicationevents.web;

import java.time.LocalDate;

public record DailyCommunicationEventStatusActivitySummaryResponse(
    LocalDate businessDate,
    long transitionCount,
    long eventCount
) {
}
