package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryContactPurposeResponse(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
