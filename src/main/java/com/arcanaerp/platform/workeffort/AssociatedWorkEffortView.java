package com.arcanaerp.platform.workeffort;

import java.util.UUID;

public record AssociatedWorkEffortView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long associatedRecordId,
    String associatedRecordType
) {
}
