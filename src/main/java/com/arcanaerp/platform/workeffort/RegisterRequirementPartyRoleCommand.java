package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record RegisterRequirementPartyRoleCommand(
    Long requirementId,
    Long partyId,
    Long roleTypeId,
    String description,
    String externalIdentifier,
    String externalIdSource,
    Instant validFrom,
    Instant validTo
) {
}
