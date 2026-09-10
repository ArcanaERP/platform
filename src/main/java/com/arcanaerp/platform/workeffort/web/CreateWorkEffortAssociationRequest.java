package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkEffortAssociationRequest(
    @NotBlank String tenantCode,
    @NotBlank String associationTypeCode,
    String description,
    @NotBlank String fromEffortNumber,
    @NotBlank String toEffortNumber,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String relationshipTypeCode,
    String effectiveFrom,
    String effectiveThru
) {
}
