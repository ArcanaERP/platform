package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;

public record AssignWorkEffortRequest(
    @NotBlank String tenantCode,
    @NotBlank String assignedTo,
    @NotBlank String reason,
    @NotBlank String assignedBy
) {
}
