package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record PartySkillResponse(
    UUID id,
    Long partyId,
    Long skillTypeId,
    Instant createdAt
) {
}
