package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryPickupDropoffTransactionView(
    UUID id,
    UUID inventoryAdjustmentId,
    String sku,
    String locationCode,
    String transactionTypeCode,
    BigDecimal quantity,
    BigDecimal quantityDelta,
    BigDecimal previousOnHandQuantity,
    BigDecimal currentOnHandQuantity,
    String reason,
    String handledBy,
    String fixedAssetCode,
    String facilityCode,
    String referenceType,
    String referenceId,
    Instant transactionAt
) {
}
