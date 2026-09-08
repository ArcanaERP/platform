package com.arcanaerp.platform.inventory;

public record RegisterInventoryTelecomContactCommand(
    String ownerType,
    String ownerCode,
    String contactPurposeCode,
    String telecomType,
    String contactName,
    String contactValue
) {
}
