package com.arcanaerp.platform.identity.web;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
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
