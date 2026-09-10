package com.arcanaerp.platform.workeffort;

public record RegisterRequirementCommand(
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
    Long deliverableId
) {
}
