package com.arcanaerp.platform.workeffort;

public record AssignWorkEffortCommand(
    String tenantCode,
    String effortNumber,
    String assignedTo,
    String reason,
    String assignedBy
) {
}
