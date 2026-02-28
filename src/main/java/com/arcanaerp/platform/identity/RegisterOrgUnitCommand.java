package com.arcanaerp.platform.identity;

public record RegisterOrgUnitCommand(
    String tenantCode,
    String tenantName,
    String code,
    String name
) {
}
