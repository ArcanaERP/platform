package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotBlank String displayName,
    @NotNull Boolean active
) {
}
