package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortFixedAssetStandardView(
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
