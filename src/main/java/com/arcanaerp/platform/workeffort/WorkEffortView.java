package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortView(
    UUID id,
    String tenantCode,
    String effortNumber,
    String name,
    String description,
    WorkEffortStatus status,
    String assignedTo,
    Instant dueAt,
    Long parentId,
    Long left,
    Long right,
    Long facilityId,
    Long projectedCostMoneyId,
    Long actualCostMoneyId,
    Long fixedAssetId,
    Long workEffortPurposeTypeId,
    Long workEffortTypeId,
    String legacyType,
    Instant startedAt,
    Instant finishedAt,
    Integer projectedCompletionTime,
    Integer actualCompletionTime,
    Long workEffortRecordId,
    String workEffortRecordType,
    Long workEffortItemId,
    String workEffortItemType,
    Instant createdAt
) {
}
