package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;

public record WorkEffortAssignmentActivitySummaryResponse(
    String tenantCode,
    String assignedTo,
    long assignmentCount,
    Instant firstAssignedAt,
    Instant lastAssignedAt
) {
}
