package com.arcanaerp.platform.identity;

import java.time.Instant;
import java.util.UUID;

public record UserView(
    UUID id,
    UUID tenantId,
    String tenantCode,
    String tenantName,
    UUID roleId,
    String roleCode,
    String roleName,
    String email,
    String displayName,
    boolean active,
    Instant createdAt
) {
}
