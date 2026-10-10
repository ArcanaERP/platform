package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateWorkEffortFixedAssetStandardRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotNull Long fixedAssetTypeId,
    BigDecimal estimatedQuantity,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId
) {
}
