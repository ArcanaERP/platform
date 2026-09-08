package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryTelecomContactResponse(
    UUID id,
    String ownerType,
    String ownerCode,
    String contactPurposeCode,
    String telecomType,
    String contactName,
    String contactValue,
    Instant createdAt,
    Instant updatedAt
) {
}
