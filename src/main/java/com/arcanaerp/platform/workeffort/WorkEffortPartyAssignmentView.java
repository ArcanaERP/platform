package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortPartyAssignmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    String partyCode,
    String roleTypeCode,
    Instant assignedFrom,
    Instant assignedThru,
    String comments,
    Instant createdAt
) {
}
