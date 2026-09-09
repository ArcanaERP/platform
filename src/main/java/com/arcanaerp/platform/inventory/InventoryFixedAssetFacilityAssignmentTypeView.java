package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetFacilityAssignmentTypeView(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
