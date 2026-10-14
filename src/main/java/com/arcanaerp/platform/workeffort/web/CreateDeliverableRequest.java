package com.arcanaerp.platform.workeffort.web;

public record CreateDeliverableRequest(
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    String deliverableRecordType,
    Long deliverableRecordId
) {
}
