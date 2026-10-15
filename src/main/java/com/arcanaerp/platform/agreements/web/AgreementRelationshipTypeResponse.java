package com.arcanaerp.platform.agreements.web;

import java.time.Instant;
import java.util.UUID;

public record AgreementRelationshipTypeResponse(
    UUID id,
    Long parentId,
    Integer left,
    Integer right,
    UUID validFromRoleTypeId,
    UUID validToRoleTypeId,
    String name,
    String description,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource,
    Instant createdAt
) {
}
