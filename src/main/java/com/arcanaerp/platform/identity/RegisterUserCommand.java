package com.arcanaerp.platform.identity;

public record RegisterUserCommand(
    String tenantCode,
    String tenantName,
    String roleCode,
    String roleName,
    String email,
    String displayName
) {
}
