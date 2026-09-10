package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotNull;

public record CreateRequirementPartyRoleRequest(
    @NotNull Long requirementId,
    @NotNull Long partyId,
    @NotNull Long roleTypeId,
    String description,
    String externalIdentifier,
    String externalIdSource,
    String validFrom,
    String validTo
) {
}
