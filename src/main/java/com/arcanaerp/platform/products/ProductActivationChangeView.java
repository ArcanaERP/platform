package com.arcanaerp.platform.products;

import java.time.Instant;
import java.util.UUID;

public record ProductActivationChangeView(
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
