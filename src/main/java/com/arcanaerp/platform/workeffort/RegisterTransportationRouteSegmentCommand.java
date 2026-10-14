package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record RegisterTransportationRouteSegmentCommand(
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
    Integer fuelUsed
) {
}
