package com.arcanaerp.platform.products.web;

import java.time.Instant;
import java.util.UUID;

public record ProductActivationChangeResponse(
    UUID id,
    String sku,
    String tenantCode,
    boolean previousActive,
    boolean currentActive,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
