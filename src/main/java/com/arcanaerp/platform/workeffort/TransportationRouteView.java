package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record TransportationRouteView(
    UUID id,
    String internalIdentifier,
    String description,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
