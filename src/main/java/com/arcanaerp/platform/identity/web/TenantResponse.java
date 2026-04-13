package com.arcanaerp.platform.identity.web;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String code,
    String name,
    Instant createdAt
) {
}
