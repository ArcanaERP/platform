package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetTypeMetadataChangeResponse(
    UUID id,
    String code,
    String previousDescription,
    String currentDescription,
    String changedBy,
    Instant changedAt
) {
}
