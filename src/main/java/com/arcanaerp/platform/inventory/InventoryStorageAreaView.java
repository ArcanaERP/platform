package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaView(
    UUID id,
    String facilityCode,
    String code,
    String name,
    String storageAreaType,
    String parentStorageAreaCode,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
