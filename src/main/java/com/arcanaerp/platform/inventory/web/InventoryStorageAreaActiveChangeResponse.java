package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaActiveChangeResponse(
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
