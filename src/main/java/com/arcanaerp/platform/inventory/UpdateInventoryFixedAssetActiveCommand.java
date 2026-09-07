package com.arcanaerp.platform.inventory;

public record UpdateInventoryFixedAssetActiveCommand(
    String code,
    boolean active,
    String changedBy
) {
}
