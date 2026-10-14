package com.arcanaerp.platform.workeffort.web;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateWorkEffortRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotBlank String name,
    @NotBlank String description,
    @NotNull WorkEffortStatus status,
    @NotBlank String assignedTo,
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
    String workEffortItemType
) {
}
