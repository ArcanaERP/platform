package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record TransportationRouteStopView(
    UUID id,
    UUID transportationRouteId,
    String internalIdentifier,
    String description,
    Long postalAddressId,
    String geoloc,
    Integer sequence,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
