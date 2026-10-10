package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortTypeAssociationView(
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
