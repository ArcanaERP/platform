package com.arcanaerp.platform.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOrgUnitRequest(
    @NotBlank String name,
    @NotNull Boolean active
) {
}
