package com.arcanaerp.platform.identity;

import java.time.Instant;
import java.util.UUID;

public record RoleView(
    UUID id,
    UUID tenantId,
    String tenantCode,
    String tenantName,
    String code,
    String name,
    Instant createdAt
) {
}
