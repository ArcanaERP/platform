package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record OrderRequirementCommitmentResponse(
    UUID id,
    Long orderLineItemId,
    Long requirementId,
    String description,
    Integer quantity,
    Instant createdAt
) {
}
