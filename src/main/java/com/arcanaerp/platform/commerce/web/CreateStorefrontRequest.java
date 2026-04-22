package com.arcanaerp.platform.commerce.web;

import jakarta.validation.constraints.NotBlank;

public record CreateStorefrontRequest(
    @NotBlank String tenantCode,
    @NotBlank String storefrontCode,
    @NotBlank String name,
    @NotBlank String currencyCode,
    @NotBlank String defaultLanguageTag,
    boolean active
) {
}
