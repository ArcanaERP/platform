package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record OrderRequirementCommitmentView(
    UUID id,
    Long orderLineItemId,
    Long requirementId,
    String description,
    Integer quantity,
    Instant createdAt
) {
}
