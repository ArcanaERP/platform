package com.arcanaerp.platform.inventory;

public record UpdateInventoryStorageAreaMetadataCommand(
    String name,
    String storageAreaType,
    String parentStorageAreaCode,
    String changedBy
) {
}
