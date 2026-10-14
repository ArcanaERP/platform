package com.arcanaerp.platform.workeffort;

public record RegisterTransportationRouteCommand(
    String internalIdentifier,
    String description,
    String comments,
    String externalIdentifier,
    String externalIdSource
) {
}
