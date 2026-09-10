package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkRequirementFulfillmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long requirementId,
    String description,
    Instant createdAt
) {
}
