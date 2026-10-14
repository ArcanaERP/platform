package com.arcanaerp.platform.workeffort.web;

public record CreateTransportationRouteRequest(
    String internalIdentifier,
    String description,
    String comments,
    String externalIdentifier,
    String externalIdSource
) {
}
