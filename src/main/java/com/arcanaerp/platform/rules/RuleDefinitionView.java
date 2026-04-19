package com.arcanaerp.platform.rules;

import java.time.Instant;
import java.util.UUID;

public record RuleDefinitionView(
    UUID id,
    String tenantCode,
    String code,
    String name,
    String appliesTo,
    String expression,
    String description,
    boolean active,
    Instant createdAt
) {
}
