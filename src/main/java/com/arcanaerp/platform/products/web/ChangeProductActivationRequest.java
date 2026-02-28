package com.arcanaerp.platform.products.web;

import jakarta.validation.constraints.NotNull;

public record ChangeProductActivationRequest(
    @NotNull Boolean active
) {
}
