package com.arcanaerp.platform.communicationevents;

import java.time.Instant;

public record CreateCommunicationEventCommand(
    String tenantCode,
    String channel,
    String direction,
    String subject,
    String summary,
    Instant occurredAt,
    String recordedBy,
    String externalReference
) {
}
