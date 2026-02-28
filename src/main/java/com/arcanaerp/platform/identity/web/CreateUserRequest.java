package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank String tenantCode,
    @NotBlank String tenantName,
    @NotBlank String roleCode,
    @NotBlank String roleName,
    @NotBlank @Email String email,
    @NotBlank String displayName
) {
}
