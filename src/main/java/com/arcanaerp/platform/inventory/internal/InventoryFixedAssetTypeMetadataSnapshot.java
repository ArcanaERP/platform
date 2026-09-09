package com.arcanaerp.platform.inventory.internal;

record InventoryFixedAssetTypeMetadataSnapshot(
    String description
) {
    static InventoryFixedAssetTypeMetadataSnapshot from(InventoryFixedAssetType type) {
        return new InventoryFixedAssetTypeMetadataSnapshot(type.getDescription());
    }
}
