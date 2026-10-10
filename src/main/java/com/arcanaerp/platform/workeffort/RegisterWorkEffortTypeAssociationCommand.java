package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortTypeAssociationCommand(
    Long workEffortTypeAssociationTypeId,
    Long fromWorkEffortTypeId,
    Long toWorkEffortTypeId,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource
) {
}
