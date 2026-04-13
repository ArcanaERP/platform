package com.arcanaerp.platform.identity;

public record RegisterRoleCommand(
    String tenantCode,
    String tenantName,
    String code,
    String name
) {
}
