package com.arcanaerp.platform.workeffort;

import java.time.Instant;

public record RegisterPartyResourceAvailabilityCommand(
    Instant fromDate,
    Instant toDate,
    Long partyId,
    Long praTypeId
) {
}
