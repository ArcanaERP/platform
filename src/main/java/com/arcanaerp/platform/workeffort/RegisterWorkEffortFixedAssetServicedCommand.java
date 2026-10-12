package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortFixedAssetServicedCommand(
    String tenantCode,
    String effortNumber,
    Long fixedAssetId
) {
}
