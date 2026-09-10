package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortAssociationView(
    UUID id,
    String tenantCode,
    String associationTypeCode,
    String description,
    UUID fromWorkEffortId,
    String fromEffortNumber,
    UUID toWorkEffortId,
    String toEffortNumber,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String relationshipTypeCode,
    Instant effectiveFrom,
    Instant effectiveThru,
    Instant createdAt
) {
}
