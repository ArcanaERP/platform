package com.arcanaerp.platform.inventory;

public record UpdateInventoryTelecomContactMetadataCommand(
    String contactPurposeCode,
    String telecomType,
    String contactName,
    String contactValue,
    String changedBy
) {
}
