package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
    @NotBlank String code,
    @NotBlank String name
) {
}
