package com.arcanaerp.platform.inventory;

public record RegisterInventoryFixedAssetTypeCommand(
    String code,
    String description
) {
}
