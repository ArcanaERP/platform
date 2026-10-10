package com.arcanaerp.platform.workeffort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortSkillStandardView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long skillTypeId,
    BigDecimal estimatedNumPeople,
    BigDecimal estimatedDuration,
    Long estimatedCostMoneyId,
    Instant createdAt
) {
}
