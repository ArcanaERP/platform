package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryTelecomContactView(
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
