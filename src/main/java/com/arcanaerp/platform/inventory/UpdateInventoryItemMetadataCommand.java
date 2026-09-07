package com.arcanaerp.platform.inventory;

public record UpdateInventoryItemMetadataCommand(
    String sku,
    String locationCode,
    String unitOfMeasurementCode,
    String classificationCode,
    String productInstanceCode,
    String externalReference,
    String sourceSystemCode,
    String ownerTenantCode,
    String ownerUserId,
    String ownerRoleCode,
    String changedBy
) {
}
