package com.arcanaerp.platform.inventory;

public record RegisterInventoryFixedAssetCommand(
    String code,
    String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource
) {
}
