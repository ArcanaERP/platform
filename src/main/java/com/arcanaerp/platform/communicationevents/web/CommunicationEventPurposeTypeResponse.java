package com.arcanaerp.platform.communicationevents.web;

import java.time.Instant;
import java.util.UUID;

public record CommunicationEventPurposeTypeResponse(
    UUID id,
    String tenantCode,
    String code,
    String name,
    Instant createdAt
) {
}
