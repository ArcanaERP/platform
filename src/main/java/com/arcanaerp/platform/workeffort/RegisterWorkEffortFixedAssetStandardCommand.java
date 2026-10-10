package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;

public record RegisterWorkEffortFixedAssetStandardCommand(
    String tenantCode,
    String effortNumber,
    Long fixedAssetTypeId,
    BigDecimal estimatedQuantity,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId
) {
}
