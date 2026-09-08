package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryPartyView(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
