package com.arcanaerp.platform.identity;

public record UpdateRoleCommand(
    String tenantCode,
    String code,
    String name
) {
}
