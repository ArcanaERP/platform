package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record RequirementTypeResponse(
    UUID id,
    Long parentId,
    Integer left,
    Integer right,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
