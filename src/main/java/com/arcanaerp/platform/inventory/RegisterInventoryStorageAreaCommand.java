package com.arcanaerp.platform.inventory;

public record RegisterInventoryStorageAreaCommand(
    String facilityCode,
    String code,
    String name,
    String storageAreaType,
    String parentStorageAreaCode
) {
}
