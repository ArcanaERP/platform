package com.arcanaerp.platform.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryTransferResponse(
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
