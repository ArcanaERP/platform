package com.arcanaerp.platform.communicationevents;

import java.time.Instant;
import java.util.UUID;

public record CommunicationEventStatusTypeView(
    UUID id,
    String tenantCode,
    String code,
    String name,
    Instant createdAt
) {
}
