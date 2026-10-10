package com.arcanaerp.platform.workeffort.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortGoodStandardResponse(
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
