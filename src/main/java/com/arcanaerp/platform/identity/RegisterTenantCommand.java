package com.arcanaerp.platform.identity;

public record RegisterTenantCommand(
    String code,
    String name
) {
}
