package com.arcanaerp.platform.workeffort;

import java.util.UUID;

public record WorkEffortAssignmentSummaryView(
    UUID id,
    String tenantCode,
    String effortNumber,
    String assignedTo
) {
}
