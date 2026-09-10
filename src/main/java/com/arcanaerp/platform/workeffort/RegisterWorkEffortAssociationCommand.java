package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record RegisterWorkEffortAssociationCommand(
    String tenantCode,
    String associationTypeCode,
    String description,
    String fromEffortNumber,
    String toEffortNumber,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String relationshipTypeCode,
    Instant effectiveFrom,
    Instant effectiveThru
) {
}
