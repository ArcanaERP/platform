package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeWorkEffortStatusRequest(
    @NotBlank String tenantCode,
    @NotNull WorkEffortStatus status,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
