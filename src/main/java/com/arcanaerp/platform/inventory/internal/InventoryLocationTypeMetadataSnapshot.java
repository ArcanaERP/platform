package com.arcanaerp.platform.inventory.internal;

record InventoryLocationTypeMetadataSnapshot(
    String description,
    String parentCode
) {
    static InventoryLocationTypeMetadataSnapshot from(InventoryLocationType type) {
        return new InventoryLocationTypeMetadataSnapshot(
            type.getDescription(),
            type.getParentCode()
        );
    }
}
