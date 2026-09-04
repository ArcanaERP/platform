package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemMetadataChangeResponse(
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
