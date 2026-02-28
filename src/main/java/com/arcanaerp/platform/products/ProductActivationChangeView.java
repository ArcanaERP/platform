package com.arcanaerp.platform.products;

import java.time.Instant;
import java.util.UUID;

public record ProductActivationChangeView(
    UUID id,
    String sku,
    boolean previousActive,
    boolean currentActive,
    String reason,
    Instant changedAt
) {
}
