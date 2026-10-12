package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record PartyResourceAvailabilityResponse(
    UUID id,
    Instant fromDate,
    Instant toDate,
    Long partyId,
    Long praTypeId,
    Instant createdAt
) {
}
