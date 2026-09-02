package com.arcanaerp.platform.communicationevents.web;

import java.time.LocalDate;

public record WeeklyCommunicationEventStatusActivitySummaryResponse(
    LocalDate businessWeekStart,
    long transitionCount,
    long eventCount
) {
}
