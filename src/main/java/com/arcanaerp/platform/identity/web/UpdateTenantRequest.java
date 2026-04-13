package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(
    @NotBlank String name
) {
}
