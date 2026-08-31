package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record WorkEffortAssignmentActivitySummaryView(
    String tenantCode,
    String assignedTo,
    long assignmentCount,
    Instant firstAssignedAt,
    Instant lastAssignedAt
) {
}
