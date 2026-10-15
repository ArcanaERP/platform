package com.arcanaerp.platform.agreements.web;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyProgramCodeResponse(
    UUID id,
    String identifier,
    String name,
    String description,
    Instant createdAt
) {
}
