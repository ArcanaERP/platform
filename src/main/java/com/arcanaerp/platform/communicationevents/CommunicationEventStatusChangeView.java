package com.arcanaerp.platform.communicationevents;

import java.time.Instant;
import java.util.UUID;

public record CommunicationEventStatusChangeView(
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
