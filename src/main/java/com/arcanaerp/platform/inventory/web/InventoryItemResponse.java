package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
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
    UUID ownerUserId,
    String ownerTenantCode,
    String ownerRoleCode,
    Instant updatedAt
) {
}
