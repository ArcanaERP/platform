package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record TransportationRouteStopResponse(
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
