package com.arcanaerp.platform.products.web;

import java.time.Instant;
import java.util.UUID;

public record ProductActivationChangeResponse(
    UUID id,
    String sku,
    boolean previousActive,
    boolean currentActive,
    String reason,
    Instant changedAt
) {
}
