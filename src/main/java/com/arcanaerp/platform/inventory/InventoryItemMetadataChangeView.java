package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemMetadataChangeView(
    UUID id,
    String sku,
    String locationCode,
    String previousUnitOfMeasurementCode,
    String currentUnitOfMeasurementCode,
    String previousClassificationCode,
    String currentClassificationCode,
    String changedBy,
    Instant changedAt
) {
}
