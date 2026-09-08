package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaActiveChangeView(
    UUID id,
    UUID storageAreaId,
    String facilityCode,
    String storageAreaCode,
    boolean previousActive,
    boolean currentActive,
    String changedBy,
    Instant changedAt
) {
}
