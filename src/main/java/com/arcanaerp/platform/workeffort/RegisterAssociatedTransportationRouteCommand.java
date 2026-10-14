package com.arcanaerp.platform.workeffort;

import java.util.UUID;

public record RegisterAssociatedTransportationRouteCommand(
    UUID transportationRouteId,
    Long associatedRecordId,
    String associatedRecordType
) {
}
