package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationTypeMetadataChangeView(
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
