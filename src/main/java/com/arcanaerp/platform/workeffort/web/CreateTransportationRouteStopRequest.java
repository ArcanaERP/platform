package com.arcanaerp.platform.workeffort.web;

import java.util.UUID;

public record CreateTransportationRouteStopRequest(
    UUID transportationRouteId,
    String internalIdentifier,
    String description,
    Long postalAddressId,
    String geoloc,
    Integer sequence,
    String externalIdentifier,
    String externalIdSource
) {
}
