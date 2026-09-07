package com.arcanaerp.platform.inventory;

import java.math.BigDecimal;

public record RecordInventoryPickupDropoffCommand(
    String sku,
    String locationCode,
    String transactionTypeCode,
    BigDecimal quantity,
    String reason,
    String handledBy,
    String referenceType,
    String referenceId
) {
}
