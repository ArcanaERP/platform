package com.arcanaerp.platform.identity;

public record UpdateTenantCommand(
    String code,
    String name
) {
}
