package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record RegisterWorkEffortPartyAssignmentCommand(
    String tenantCode,
    String effortNumber,
    String partyCode,
    String roleTypeCode,
    Instant assignedFrom,
    Instant assignedThru,
    String comments
) {
}
