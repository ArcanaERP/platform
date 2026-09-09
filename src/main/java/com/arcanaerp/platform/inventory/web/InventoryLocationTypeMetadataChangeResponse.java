package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationTypeMetadataChangeResponse(
    UUID id,
    String code,
    String previousDescription,
    String currentDescription,
    String previousParentCode,
    String currentParentCode,
    String changedBy,
    Instant changedAt
) {
}
