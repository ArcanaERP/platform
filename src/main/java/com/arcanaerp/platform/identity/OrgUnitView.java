package com.arcanaerp.platform.identity;

import java.time.Instant;
import java.util.UUID;

public record OrgUnitView(
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
