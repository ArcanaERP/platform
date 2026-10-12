package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortFixedAssetServicedView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long fixedAssetId,
    Instant createdAt
) {
}
