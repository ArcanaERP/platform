package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortAssignmentChangeView(
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
