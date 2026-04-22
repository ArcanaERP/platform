package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortView(
    UUID id,
    String tenantCode,
    String effortNumber,
    String name,
    String description,
    WorkEffortStatus status,
    String assignedTo,
    Instant dueAt,
    Instant createdAt
) {
}
