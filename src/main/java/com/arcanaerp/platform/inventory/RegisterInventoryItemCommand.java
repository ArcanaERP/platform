package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record RegisterInventoryItemCommand(
    String sku,
    String locationCode,
    BigDecimal onHandQuantity,
    String unitOfMeasurementCode,
    String classificationCode
) {
}
