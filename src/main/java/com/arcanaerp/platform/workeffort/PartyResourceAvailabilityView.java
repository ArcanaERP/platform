package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record PartyResourceAvailabilityView(
    UUID id,
    Instant fromDate,
    Instant toDate,
    Long partyId,
    Long praTypeId,
    Instant createdAt
) {
}
