package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateWorkEffortSkillStandardRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotNull Long skillTypeId,
    BigDecimal estimatedNumPeople,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId
) {
}
