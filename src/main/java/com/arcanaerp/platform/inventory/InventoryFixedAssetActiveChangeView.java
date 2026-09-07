package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetActiveChangeView(
    UUID id,
    String fixedAssetCode,
    boolean previousActive,
    boolean currentActive,
    String changedBy,
    Instant changedAt
) {
}
