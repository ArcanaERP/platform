package com.arcanaerp.platform.inventory.internal;

record InventoryTelecomContactMetadataSnapshot(
    String contactPurposeCode,
    String telecomType,
    String contactName,
    String contactValue
) {
    static InventoryTelecomContactMetadataSnapshot from(InventoryTelecomContact contact) {
        return new InventoryTelecomContactMetadataSnapshot(
            contact.getContactPurposeCode(),
            contact.getTelecomType(),
            contact.getContactName(),
            contact.getContactValue()
        );
    }
}
