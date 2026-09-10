package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortRoleTypeAssignmentCommand(
    String tenantCode,
    String effortNumber,
    String roleTypeCode
) {
}
