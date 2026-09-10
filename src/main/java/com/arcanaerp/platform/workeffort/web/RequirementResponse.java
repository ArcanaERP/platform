package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record RequirementResponse(
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
