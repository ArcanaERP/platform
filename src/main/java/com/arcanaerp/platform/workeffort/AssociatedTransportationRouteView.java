package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record AssociatedTransportationRouteView(
    UUID id,
    UUID transportationRouteId,
    Long associatedRecordId,
    String associatedRecordType,
    Instant createdAt
) {
}
