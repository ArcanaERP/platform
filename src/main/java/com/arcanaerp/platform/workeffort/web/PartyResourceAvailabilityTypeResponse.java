package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record PartyResourceAvailabilityTypeResponse(
    UUID id,
    String description,
    String internalIdentifier,
    Instant createdAt
) {
}
