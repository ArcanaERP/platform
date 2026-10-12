package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record PartyResourceAvailabilityTypeView(
    UUID id,
    String description,
    String internalIdentifier,
    Instant createdAt
) {
}
