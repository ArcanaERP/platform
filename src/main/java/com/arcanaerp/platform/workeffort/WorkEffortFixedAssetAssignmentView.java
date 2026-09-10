package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortFixedAssetAssignmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String fixedAssetCode,
    Instant createdAt
) {
}
