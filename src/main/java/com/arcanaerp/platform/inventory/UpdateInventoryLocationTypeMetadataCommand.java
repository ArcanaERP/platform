package com.arcanaerp.platform.inventory;

public record UpdateInventoryLocationTypeMetadataCommand(
    String code,
    String description,
    String parentCode,
    String changedBy
) {
}
