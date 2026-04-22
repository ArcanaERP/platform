package com.arcanaerp.platform.commerce;

import java.time.Instant;
import java.util.UUID;

public record StorefrontView(
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
