package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
    @NotBlank String tenantCode,
    @NotBlank String tenantName,
    @NotBlank String code,
    @NotBlank String name
) {
}
