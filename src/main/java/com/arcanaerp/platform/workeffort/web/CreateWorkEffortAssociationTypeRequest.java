package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkEffortAssociationTypeRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    String parentTypeCode,
    String validFromRoleTypeCode,
    String validToRoleTypeCode,
    String externalIdentifier,
    String externalIdSource
) {
}
