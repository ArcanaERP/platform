package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortTypeAssociationResponse(
    UUID id,
    Long workEffortTypeAssociationTypeId,
    Long fromWorkEffortTypeId,
    Long toWorkEffortTypeId,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
