package com.arcanaerp.platform.inventory.internal;

record InventoryFixedAssetMetadataSnapshot(
    String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource
) {
    static InventoryFixedAssetMetadataSnapshot from(InventoryFixedAsset fixedAsset) {
        return new InventoryFixedAssetMetadataSnapshot(
            fixedAsset.getDescription(),
            fixedAsset.getFixedAssetTypeCode(),
            fixedAsset.getComments(),
            fixedAsset.getExternalIdentifier(),
            fixedAsset.getExternalIdSource()
        );
    }
}
