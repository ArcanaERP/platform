package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryPartyResponse(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
