package com.arcanaerp.platform.inventory;

public record UpdateInventoryFixedAssetTypeMetadataCommand(
    String code,
    String description,
    String changedBy
) {
}
