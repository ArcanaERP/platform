package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record PartySkillView(
    UUID id,
    Long partyId,
    Long skillTypeId,
    Instant createdAt
) {
}
