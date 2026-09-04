package com.arcanaerp.platform.inventory;

public record UpdateInventoryItemMetadataCommand(
    String sku,
    String locationCode,
    String unitOfMeasurementCode,
    String classificationCode,
    String changedBy
) {
}
