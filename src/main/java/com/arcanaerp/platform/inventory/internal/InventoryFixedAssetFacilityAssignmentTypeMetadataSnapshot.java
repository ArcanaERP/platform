package com.arcanaerp.platform.inventory.internal;

record InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot(
    String description
) {
    static InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot from(
        InventoryFixedAssetFacilityAssignmentType type
    ) {
        return new InventoryFixedAssetFacilityAssignmentTypeMetadataSnapshot(type.getDescription());
    }
}
