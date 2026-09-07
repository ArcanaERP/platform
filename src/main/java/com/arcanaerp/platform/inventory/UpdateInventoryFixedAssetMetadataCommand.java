package com.arcanaerp.platform.inventory;

public record UpdateInventoryFixedAssetMetadataCommand(
    String code,
    String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    String changedBy
) {
}
