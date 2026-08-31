package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortAssignmentChangeResponse(
    UUID id,
    String effortNumber,
    String previousAssignedTo,
    String currentAssignedTo,
    String tenantCode,
    String reason,
    String assignedBy,
    Instant assignedAt
) {
}
