package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record CreateWorkEffortCommand(
    String tenantCode,
    String effortNumber,
    String name,
    String description,
    WorkEffortStatus status,
    String assignedTo,
    Instant dueAt
) {
}
