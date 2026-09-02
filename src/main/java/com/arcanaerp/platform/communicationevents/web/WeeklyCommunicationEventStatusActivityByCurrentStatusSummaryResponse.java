package com.arcanaerp.platform.communicationevents.web;

import java.time.LocalDate;

public record WeeklyCommunicationEventStatusActivityByCurrentStatusSummaryResponse(
    LocalDate businessWeekStart,
    String currentStatusCode,
    String currentStatusName,
    long transitionCount,
    long eventCount
) {
}
