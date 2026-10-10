package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateWorkEffortGoodStandardRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotNull Long goodTypeId,
    BigDecimal estimatedQuantity,
    Long estimatedCostMoneyId
) {
}
