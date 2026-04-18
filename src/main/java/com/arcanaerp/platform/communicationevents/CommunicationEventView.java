package com.arcanaerp.platform.communicationevents;

import java.time.Instant;
import java.util.UUID;

public record CommunicationEventView(
    UUID id,
    String eventNumber,
    String tenantCode,
    CommunicationChannel channel,
    CommunicationDirection direction,
    String subject,
    String summary,
    Instant occurredAt,
    String recordedBy,
    String externalReference,
    Instant createdAt
) {
}
