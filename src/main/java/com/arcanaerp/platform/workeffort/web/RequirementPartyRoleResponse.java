package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record RequirementPartyRoleResponse(
    UUID id,
    Long requirementId,
    Long partyId,
    Long roleTypeId,
    String description,
    String externalIdentifier,
    String externalIdSource,
    Instant validFrom,
    Instant validTo,
    Instant createdAt
) {
}
