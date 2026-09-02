package com.arcanaerp.platform.communicationevents.web;

import java.time.LocalDate;

public record DailyCommunicationEventStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessDate,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
