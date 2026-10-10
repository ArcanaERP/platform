package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotNull;

public record CreateWorkEffortTypeAssociationRequest(
    @NotNull Long workEffortTypeAssociationTypeId,
    @NotNull Long fromWorkEffortTypeId,
    @NotNull Long toWorkEffortTypeId,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource
) {
}
