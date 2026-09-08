package com.arcanaerp.platform.inventory.internal;

record InventoryStorageAreaMetadataSnapshot(
    String name,
    String storageAreaType,
    String parentStorageAreaCode
) {
    static InventoryStorageAreaMetadataSnapshot from(InventoryStorageArea storageArea) {
        return new InventoryStorageAreaMetadataSnapshot(
            storageArea.getName(),
            storageArea.getStorageAreaType(),
            storageArea.getParentStorageAreaCode()
        );
    }
}
