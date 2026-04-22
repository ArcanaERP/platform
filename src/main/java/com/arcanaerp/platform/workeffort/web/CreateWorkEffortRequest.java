package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateWorkEffortRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotBlank String name,
    @NotBlank String description,
    @NotNull WorkEffortStatus status,
    @NotBlank String assignedTo,
    Instant dueAt
) {
}
