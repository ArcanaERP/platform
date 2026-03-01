package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryTransferView(
    UUID transferId,
    String sku,
    String sourceLocationCode,
    String destinationLocationCode,
    BigDecimal quantity,
    BigDecimal sourceOnHandQuantity,
    BigDecimal destinationOnHandQuantity,
    String reason,
    String adjustedBy,
    Instant transferredAt
) {
}
