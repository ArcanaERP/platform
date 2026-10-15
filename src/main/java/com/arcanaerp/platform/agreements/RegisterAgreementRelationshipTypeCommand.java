package com.arcanaerp.platform.agreements;

import java.util.UUID;

public record RegisterAgreementRelationshipTypeCommand(
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
