package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record RequirementView(
    UUID id,
    Long parentId,
    Integer left,
    Integer right,
    String description,
    String type,
    Integer projectedCompletionTime,
    Long estimatedBudgetMoneyId,
    Long requirementTypeId,
    Long requirementRecordId,
    String requirementRecordType,
    Long fixedAssetId,
    Long productId,
    Long deliverableId,
    Instant createdAt
) {
}
