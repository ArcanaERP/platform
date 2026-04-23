package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateDiagnosticRunLogRequest(
    @NotBlank String tenantCode,
    @NotBlank String runNumber,
    @NotBlank String diagnosticCode,
    @NotBlank String title,
    @NotBlank String summary,
    @NotNull DiagnosticRunStatus status,
    @NotNull Instant startedAt,
    Instant finishedAt
) {
}
