package com.arcanaerp.platform.commerce.web;

import java.time.Instant;
import java.util.UUID;

public record StorefrontResponse(
    UUID id,
    String tenantCode,
    String storefrontCode,
    String name,
    String currencyCode,
    String defaultLanguageTag,
    boolean active,
    Instant createdAt
) {
}
