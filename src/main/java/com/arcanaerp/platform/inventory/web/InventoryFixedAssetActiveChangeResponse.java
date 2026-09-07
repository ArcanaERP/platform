package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetActiveChangeResponse(
    UUID id,
    String fixedAssetCode,
    boolean previousActive,
    boolean currentActive,
    String changedBy,
    Instant changedAt
) {
}
