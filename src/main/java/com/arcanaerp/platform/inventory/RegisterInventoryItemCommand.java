package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record RegisterInventoryItemCommand(
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
    String ownerUserId,
    String ownerRoleCode
) {
}
