package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaMetadataChangeResponse(
    UUID id,
    UUID storageAreaId,
    String facilityCode,
    String storageAreaCode,
    String previousName,
    String currentName,
    String previousStorageAreaType,
    String currentStorageAreaType,
    String previousParentStorageAreaCode,
    String currentParentStorageAreaCode,
    String changedBy,
    Instant changedAt
) {
}
