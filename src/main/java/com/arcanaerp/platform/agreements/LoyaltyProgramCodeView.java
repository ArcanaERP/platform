package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyProgramCodeView(
    UUID id,
    String identifier,
    String name,
    String description,
    Instant createdAt
) {
}
