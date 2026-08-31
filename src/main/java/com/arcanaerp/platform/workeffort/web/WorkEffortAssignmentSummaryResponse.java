package com.arcanaerp.platform.workeffort.web;

import java.util.UUID;

public record WorkEffortAssignmentSummaryResponse(
    UUID id,
    String tenantCode,
    String effortNumber,
    String assignedTo
) {
}
