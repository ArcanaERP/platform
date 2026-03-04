package com.arcanaerp.platform.identity.web;

import java.time.Instant;
import java.util.UUID;

public record OrgUnitResponse(
    UUID id,
    UUID tenantId,
    String tenantCode,
    String tenantName,
    String code,
    String name,
    boolean active,
    Instant createdAt
) {
}
