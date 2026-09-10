package com.arcanaerp.platform.workeffort;

import java.util.UUID;

public record WorkEffortRoleTypeAssignmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String roleTypeCode
) {
}
