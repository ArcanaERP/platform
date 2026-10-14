package com.arcanaerp.platform.workeffort;

public record RegisterDeliverableCommand(
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    String deliverableRecordType,
    Long deliverableRecordId
) {
}
