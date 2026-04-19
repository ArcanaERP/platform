package com.arcanaerp.platform.communicationevents.web;

import java.time.Instant;
import java.util.UUID;

public record CommunicationEventStatusChangeResponse(
    UUID id,
    String eventNumber,
    String previousStatusCode,
    String previousStatusName,
    String currentStatusCode,
    String currentStatusName,
    String tenantCode,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
