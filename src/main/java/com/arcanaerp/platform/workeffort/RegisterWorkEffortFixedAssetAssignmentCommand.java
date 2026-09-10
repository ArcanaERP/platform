package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortFixedAssetAssignmentCommand(
    String tenantCode,
    String effortNumber,
    String fixedAssetCode
) {
}
