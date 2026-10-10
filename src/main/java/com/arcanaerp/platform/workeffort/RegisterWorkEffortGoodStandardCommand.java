package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;

public record RegisterWorkEffortGoodStandardCommand(
    String tenantCode,
    String effortNumber,
    Long goodTypeId,
    BigDecimal estimatedQuantity,
    Long estimatedCostMoneyId
) {
}
