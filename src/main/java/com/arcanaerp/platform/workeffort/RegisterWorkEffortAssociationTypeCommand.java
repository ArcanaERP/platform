package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortAssociationTypeCommand(
    String code,
    String name,
    String description,
    String parentTypeCode,
    String validFromRoleTypeCode,
    String validToRoleTypeCode,
    String externalIdentifier,
    String externalIdSource
) {
}
