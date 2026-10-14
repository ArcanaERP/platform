package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record TransportationRouteResponse(
    UUID id,
    String internalIdentifier,
    String description,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
