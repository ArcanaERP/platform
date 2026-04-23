package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortStatusChangeResponse(
    UUID id,
    String effortNumber,
    WorkEffortStatus previousStatus,
    WorkEffortStatus currentStatus,
    String tenantCode,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
