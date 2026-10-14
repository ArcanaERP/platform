package com.arcanaerp.platform.workeffort;

import java.util.UUID;

public record RegisterTransportationRouteStopCommand(
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
