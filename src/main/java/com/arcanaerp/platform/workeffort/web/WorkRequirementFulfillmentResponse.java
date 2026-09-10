package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkRequirementFulfillmentResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long requirementId,
    String description,
    Instant createdAt
) {
}
