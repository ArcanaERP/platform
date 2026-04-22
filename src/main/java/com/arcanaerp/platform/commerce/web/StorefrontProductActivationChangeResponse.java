package com.arcanaerp.platform.commerce.web;

import java.time.Instant;
import java.util.UUID;

public record StorefrontProductActivationChangeResponse(
    UUID id,
    String tenantCode,
    String storefrontCode,
    String sku,
    boolean previousActive,
    boolean currentActive,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
