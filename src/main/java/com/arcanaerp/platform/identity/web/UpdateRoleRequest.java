package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
    @NotBlank String name
) {
}
