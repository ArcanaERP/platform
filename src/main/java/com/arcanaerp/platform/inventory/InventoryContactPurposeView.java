package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryContactPurposeView(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
