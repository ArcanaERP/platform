package com.arcanaerp.platform.identity;

public record UpdateOrgUnitCommand(
    String tenantCode,
    String code,
    String name,
    boolean active
) {
}
