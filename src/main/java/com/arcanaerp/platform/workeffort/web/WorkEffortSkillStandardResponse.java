package com.arcanaerp.platform.workeffort.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WorkEffortSkillStandardResponse(
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
