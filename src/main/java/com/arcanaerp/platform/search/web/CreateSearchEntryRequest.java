package com.arcanaerp.platform.search.web;

import jakarta.validation.constraints.NotBlank;

public record CreateSearchEntryRequest(
    @NotBlank String tenantCode,
    @NotBlank String entryNumber,
    @NotBlank String title,
    @NotBlank String snippet,
    @NotBlank String category,
    @NotBlank String targetType,
    @NotBlank String targetIdentifier,
    @NotBlank String targetUri
) {
}
