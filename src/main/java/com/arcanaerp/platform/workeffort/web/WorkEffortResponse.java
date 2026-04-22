package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortResponse(
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
