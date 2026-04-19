package com.arcanaerp.platform.rules.web;

import java.time.Instant;
import java.util.UUID;

public record RuleDefinitionResponse(
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
