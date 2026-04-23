package com.arcanaerp.platform.devsupport;

import java.time.Instant;

public record RegisterDiagnosticRunLogCommand(
    String tenantCode,
    String runNumber,
    String diagnosticCode,
    String title,
    String summary,
    DiagnosticRunStatus status,
    Instant startedAt,
    Instant finishedAt
) {
}
