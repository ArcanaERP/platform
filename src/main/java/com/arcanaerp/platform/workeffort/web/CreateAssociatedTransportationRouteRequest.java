package com.arcanaerp.platform.workeffort.web;

import java.util.UUID;

public record CreateAssociatedTransportationRouteRequest(
    UUID transportationRouteId,
    Long associatedRecordId,
    String associatedRecordType
) {
}
