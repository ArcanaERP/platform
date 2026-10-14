package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record TransportationRouteSegmentResponse(
    UUID id,
    UUID transportationRouteId,
    UUID fromTransportationRouteStopId,
    UUID toTransportationRouteStopId,
    String internalIdentifier,
    String description,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    Integer sequence,
    Instant estimatedStart,
    Instant estimatedArrival,
    Instant actualStart,
    Instant actualArrival,
    Integer startMileage,
    Integer endMileage,
    Integer fuelUsed,
    Instant createdAt
) {
}
