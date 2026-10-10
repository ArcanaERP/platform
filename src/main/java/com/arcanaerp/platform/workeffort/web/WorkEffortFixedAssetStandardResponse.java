package com.arcanaerp.platform.workeffort.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortFixedAssetStandardResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long fixedAssetTypeId,
    BigDecimal estimatedQuantity,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId,
    Instant createdAt
) {
}
