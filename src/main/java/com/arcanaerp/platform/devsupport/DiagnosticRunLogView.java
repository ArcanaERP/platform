package com.arcanaerp.platform.devsupport;

import java.time.Instant;
import java.util.UUID;

public record DiagnosticRunLogView(
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
