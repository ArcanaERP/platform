package com.arcanaerp.platform.inventory;

public record UpdateInventoryLocationActiveCommand(
    String code,
    boolean active
) {
}
