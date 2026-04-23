package com.arcanaerp.platform.workeffort;

public record ChangeWorkEffortStatusCommand(
    String tenantCode,
    String effortNumber,
    WorkEffortStatus status,
    String reason,
    String changedBy
) {
}
