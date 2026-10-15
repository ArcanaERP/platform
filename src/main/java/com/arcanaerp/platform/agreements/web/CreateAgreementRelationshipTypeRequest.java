package com.arcanaerp.platform.agreements.web;

import java.util.UUID;

public record CreateAgreementRelationshipTypeRequest(
    Long parentId,
    Integer left,
    Integer right,
    UUID validFromRoleTypeId,
    UUID validToRoleTypeId,
    String name,
    String description,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource
) {
}
