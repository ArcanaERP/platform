package com.arcanaerp.platform.workeffort.web;

import java.util.UUID;

public record AssociatedWorkEffortResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long associatedRecordId,
    String associatedRecordType
) {
}
