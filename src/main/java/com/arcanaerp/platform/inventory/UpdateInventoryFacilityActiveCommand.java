package com.arcanaerp.platform.inventory;

public record UpdateInventoryFacilityActiveCommand(
    String code,
    boolean active,
    String changedBy
) {
}
