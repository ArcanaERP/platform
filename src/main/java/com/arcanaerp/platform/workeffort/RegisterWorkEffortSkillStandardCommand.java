package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;

public record RegisterWorkEffortSkillStandardCommand(
    String tenantCode,
    String effortNumber,
    Long skillTypeId,
    BigDecimal estimatedNumPeople,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId
) {
}
