package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record DeliverableView(
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
