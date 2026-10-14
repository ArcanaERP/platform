package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record AssociatedTransportationRouteResponse(
    UUID id,
    UUID transportationRouteId,
    Long associatedRecordId,
    String associatedRecordType,
    Instant createdAt
) {
}
