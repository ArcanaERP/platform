package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkEffortPartyAssignmentRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotBlank String partyCode,
    @NotBlank String roleTypeCode,
    String assignedFrom,
    String assignedThru,
    String comments
) {
}
