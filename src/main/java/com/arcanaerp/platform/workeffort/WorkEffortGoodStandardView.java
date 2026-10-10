package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortGoodStandardView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long goodTypeId,
    BigDecimal estimatedQuantity,
    Long estimatedCostMoneyId,
    Instant createdAt
) {
}
