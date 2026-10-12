package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortFixedAssetServicedResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long fixedAssetId,
    Instant createdAt
) {
}
