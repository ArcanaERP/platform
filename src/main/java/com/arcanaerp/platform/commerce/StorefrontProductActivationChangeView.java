package com.arcanaerp.platform.commerce;

import java.time.Instant;
import java.util.UUID;

public record StorefrontProductActivationChangeView(
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
