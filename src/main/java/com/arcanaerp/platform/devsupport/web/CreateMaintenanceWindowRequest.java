package com.arcanaerp.platform.devsupport.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateMaintenanceWindowRequest(
    @NotBlank String tenantCode,
    @NotBlank String windowCode,
    @NotBlank String title,
    @NotBlank String description,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    boolean active
) {
}
