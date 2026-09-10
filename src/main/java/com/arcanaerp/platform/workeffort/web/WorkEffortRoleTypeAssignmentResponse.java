package com.arcanaerp.platform.workeffort.web;

import java.util.UUID;

public record WorkEffortRoleTypeAssignmentResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String roleTypeCode
) {
}
