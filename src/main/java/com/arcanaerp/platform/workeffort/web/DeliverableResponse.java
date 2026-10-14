package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record DeliverableResponse(
    UUID id,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    String deliverableRecordType,
    Long deliverableRecordId,
    Instant createdAt
) {
}
