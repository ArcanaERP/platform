package com.arcanaerp.platform.inventory;

public record UpdateInventoryStorageAreaActiveCommand(
    boolean active,
    String changedBy
) {
}
