package com.arcanaerp.platform.communicationevents.web;

import com.arcanaerp.platform.communicationevents.CommunicationChannel;
import com.arcanaerp.platform.communicationevents.CommunicationDirection;
import java.time.Instant;
import java.util.UUID;

public record CommunicationEventResponse(
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
