package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.devsupport.DiagnosticRunStatus;
import java.time.Instant;
import java.util.UUID;

public record DiagnosticRunLogResponse(
    UUID id,
    String tenantCode,
    String runNumber,
    String diagnosticCode,
    String title,
    String summary,
    DiagnosticRunStatus status,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt
) {
}
