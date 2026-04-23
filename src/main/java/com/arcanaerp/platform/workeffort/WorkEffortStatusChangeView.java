package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortStatusChangeView(
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
