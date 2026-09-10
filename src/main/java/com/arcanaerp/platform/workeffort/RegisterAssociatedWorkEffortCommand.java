package com.arcanaerp.platform.workeffort;

public record RegisterAssociatedWorkEffortCommand(
    String tenantCode,
    String effortNumber,
    Long associatedRecordId,
    String associatedRecordType
) {
}
