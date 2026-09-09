package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetFacilityAssignmentTypeMetadataChangeView(
    UUID id,
    String code,
    String previousDescription,
    String currentDescription,
    String changedBy,
    Instant changedAt
) {
}
