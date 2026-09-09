package com.arcanaerp.platform.inventory;

public record UpdateInventoryFixedAssetFacilityAssignmentTypeMetadataCommand(
    String code,
    String description,
    String changedBy
) {
}
