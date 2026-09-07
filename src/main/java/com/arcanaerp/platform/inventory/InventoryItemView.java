package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemView(
    UUID id,
    String sku,
    String locationCode,
    BigDecimal onHandQuantity,
    BigDecimal availableQuantity,
    BigDecimal soldQuantity,
    String unitOfMeasurementCode,
    String classificationCode,
    String productInstanceCode,
    String externalReference,
    String sourceSystemCode,
    String ownerTenantCode,
    UUID ownerUserId,
    String ownerRoleCode,
    Instant updatedAt
) {
}
