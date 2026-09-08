package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaResponse(
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
