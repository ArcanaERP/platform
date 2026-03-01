package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record TransferInventoryCommand(
    String sku,
    String sourceLocationCode,
    String destinationLocationCode,
    BigDecimal quantity,
    String reason,
    String adjustedBy,
    String referenceType,
    String referenceId
) {
}
