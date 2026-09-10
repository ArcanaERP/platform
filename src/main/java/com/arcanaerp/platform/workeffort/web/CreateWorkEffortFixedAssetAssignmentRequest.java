package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkEffortFixedAssetAssignmentRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotBlank String fixedAssetCode
) {
}
