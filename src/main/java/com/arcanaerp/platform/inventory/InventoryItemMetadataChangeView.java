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
    String previousProductInstanceCode,
    String currentProductInstanceCode,
    String previousExternalReference,
    String currentExternalReference,
    String previousSourceSystemCode,
    String currentSourceSystemCode,
    String previousOwnerTenantCode,
    String currentOwnerTenantCode,
    UUID previousOwnerUserId,
    UUID currentOwnerUserId,
    String previousOwnerRoleCode,
    String currentOwnerRoleCode,
    String changedBy,
    Instant changedAt
) {
}
